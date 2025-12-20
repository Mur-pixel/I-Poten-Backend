package com.cygnus.ipoten.batch.quiz;

import com.cygnus.ipoten.quiz.entity.Quiz;
import com.cygnus.ipoten.quiz.repository.QuizRepository;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.quiz_set.entity.QuizSet;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.quiz_set.repository.QuizSetRepository;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term_category.repository.TermCategoryRepository;
import com.cygnus.ipoten.term_topic_tag.entity.TopicTag;
import com.cygnus.ipoten.term_topic_tag.repository.TopicTagRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizTsvImportRunner implements CommandLineRunner {

    private final QuizSetRepository quizSetRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final TermCategoryRepository termCategoryRepository;
    private final TermRepository termRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;
    private final QuizRepository quizRepository;
    private final TopicTagRepository topicTagRepository;
    private final TransactionTemplate txTemplate;
    private final EntityManager em;

    // ✅ row 단위 트랜잭션에서도 안전하게 쓰기 위해 "엔티티" 대신 "ID" 캐시
    private final Map<String, Long> setIdCache = new LinkedHashMap<>();
    private final Map<String, Long> termIdCache = new HashMap<>();
    private final Map<String, Long> topicTagIdCache = new HashMap<>();

    // Term별로 이미 붙인 tagKey (row 반복 중 중복 attach 방지)
    private final Map<Long, Set<String>> appliedByTerm = new HashMap<>();

    @Override
    public void run(String... args) throws Exception {
        String quizPath = null;
        for (String arg : args) {
            if (arg.startsWith("--quiz=")) {
                quizPath = arg.substring("--quiz=".length());
            }
        }
        if (quizPath == null) {
            log.info("[QUIZ-IMPORT] skipped (no --quiz=...)");
            return;
        }

        try {
            importQuizzes(quizPath);
        } catch (Exception e) {
            // 애플리케이션 부팅이 죽지 않게 보호
            log.error("[QUIZ-IMPORT] fatal error while importing quizzes from {}.", quizPath, e);
        }
    }

    private void importQuizzes(String quizPath) throws Exception {
        // 캐시 초기화
        setIdCache.clear();
        termIdCache.clear();
        topicTagIdCache.clear();
        appliedByTerm.clear();

        File file = new File(quizPath);
        if (!file.exists()) {
            log.error("[QUIZ-IMPORT] File not found: {}", quizPath);
            return;
        }

        log.info("[QUIZ-IMPORT] Importing quizzes from {}", quizPath);

        int lineNo = 0;
        int ok = 0, skip = 0, err = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("UTF-8")))) {
            String header = br.readLine(); // 첫 줄은 헤더
            lineNo++;
            Map<String, Integer> col = buildHeaderIndex(header);

            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) { skip++; continue; }

                String[] c = line.split("\t", -1);

                try {
                    final int currentLineNo = lineNo;

                    txTemplate.executeWithoutResult(status -> {
                        QuizImportRow row = parseRow(c, col);

                        // ✅ QuizSet: title -> setId 캐시
                        String setTitle = nvl(row.getQuizSetTitle(), "퀴즈 세트");
                        Long setId = setIdCache.computeIfAbsent(setTitle, k -> resolveOrCreateSetId(row, setTitle));
                        QuizSet setRef = em.getReference(QuizSet.class, setId);

                        createOneQuestion(setRef, row);
                    });

                    ok++;

                } catch (Exception ex) {
                    err++;
                    log.warn("[QUIZ-IMPORT] line {} failed: {}", lineNo, ex.getMessage(), ex);

                    // row 단위 트랜잭션이라도, 영속성 컨텍스트가 꼬였을 때 안전하게 비워줌
                    try { em.clear(); } catch (Exception ignore) {}
                }
            }
        }

        log.info("[QUIZ-IMPORT] done. ok={}, skip={}, err={}", ok, skip, err);
    }

    // =============================
    // QuizSet resolve/create (ID 반환)
    // =============================

    private Long resolveOrCreateSetId(QuizImportRow row, String title) {
        return quizSetRepository.findFirstByTitle(title)
                .map(QuizSet::getId)
                .orElseGet(() -> {
                    QuizSetType setType = QuizSetType.CHOICE;
                    String rawType = row.getQuizSetType();
                    if (rawType != null && !rawType.isBlank()) {
                        try { setType = QuizSetType.fromParam(rawType); } catch (Exception ignore) {}
                    }

                    Quiz quiz = quizRepository.save(Quiz.create(title, setType));
                    QuizSet newSet = quizSetRepository.save(QuizSet.create(quiz, title, setType, null));

                    log.info("[QUIZ-IMPORT] Created new QuizSet. title={}, id={}", title, newSet.getId());
                    return newSet.getId();
                });
    }

    // =============================
    // Header parsing
    // =============================

    private Map<String, Integer> buildHeaderIndex(String header) {
        if (header == null) throw new IllegalArgumentException("헤더가 비어 있습니다.");
        String[] heads = header.split("\t");
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < heads.length; i++) {
            map.put(heads[i].trim().toLowerCase(Locale.ROOT), i);
        }

        // 최소 컬럼 검증
        require(map, "quiz_set_title");
        require(map, "question_type");
        require(map, "question_text");
        require(map, "difficulty");

        return map;
    }

    private void require(Map<String, Integer> map, String key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("헤더 누락: " + key);
        }
    }

    private String get(String[] c, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        if (idx == null || idx < 0 || idx >= c.length) return "";
        return c[idx] == null ? "" : c[idx].trim();
    }

    private Boolean parseBool(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().toLowerCase(Locale.ROOT);
        return ("true".equals(v) || "1".equals(v) || "y".equals(v) || "yes".equals(v));
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        return Long.valueOf(s);
    }

    private String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    // =============================
    // Row parsing
    // =============================

    private QuizImportRow parseRow(String[] c, Map<String, Integer> col) {

        String quizKey       = get(c, col, "quiz_key");
        String quizTitle     = get(c, col, "quiz_title");

        String quizSetKey    = get(c, col, "quiz_set_key");
        String quizSetTitle  = get(c, col, "quiz_set_title");
        String quizSetType   = get(c, col, "quiz_set_type");

        String questionKey   = get(c, col, "question_key");
        String questionType  = get(c, col, "question_type");
        String difficulty    = get(c, col, "difficulty");
        String questionText  = get(c, col, "question_text");
        String explanation   = get(c, col, "explanation");

        Long   termId        = parseLong(get(c, col, "term_id"));
        String termTitle     = get(c, col, "term_title");
        Long   termCategoryId= parseLong(get(c, col, "term_category_id"));
        String termTopicTagKeys = get(c, col, "term_topic_tag_keys");

        String  choice1Text      = get(c, col, "choice_1_text");
        Boolean choice1IsAnswer  = parseBool(get(c, col, "choice_1_is_answer"));

        String  choice2Text      = get(c, col, "choice_2_text");
        Boolean choice2IsAnswer  = parseBool(get(c, col, "choice_2_is_answer"));

        String  choice3Text      = get(c, col, "choice_3_text");
        Boolean choice3IsAnswer  = parseBool(get(c, col, "choice_3_is_answer"));

        String  choice4Text      = get(c, col, "choice_4_text");
        Boolean choice4IsAnswer  = parseBool(get(c, col, "choice_4_is_answer"));

        String textAnswer    = get(c, col, "text_answer");
        String memo          = get(c, col, "memo");

        return new QuizImportRow(
                quizKey, quizTitle,
                quizSetKey, quizSetTitle, quizSetType,
                questionKey, questionType, difficulty, questionText, explanation,
                termId, termTitle, termCategoryId,
                termTopicTagKeys,
                choice1Text, choice1IsAnswer,
                choice2Text, choice2IsAnswer,
                choice3Text, choice3IsAnswer,
                choice4Text, choice4IsAnswer,
                textAnswer, memo
        );
    }

    // =============================
    // Term/Category resolve
    // =============================

    private TermCategory resolveCategory(Long termCategoryId) {
        if (termCategoryId == null) return null;
        return termCategoryRepository.findById(termCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("없는 term_category_id=" + termCategoryId));
    }

    private Long resolveOrCreateTermId(QuizImportRow row, TermCategory cat) {
        String title = safe(row.getTermTitle());
        if (title.isBlank()) return null;

        Long cid = (cat == null ? null : cat.getId());
        String cacheKey = title.toLowerCase(Locale.ROOT) + "#" + (cid == null ? "null" : cid);

        return termIdCache.computeIfAbsent(cacheKey, k -> {
            Term t;

            if (cid != null) {
                t = termRepository.findFirstByTitleAndTermCategory_Id(title, cid)
                        .orElseGet(() -> termRepository.save(
                                Term.builder()
                                        .title(title)
                                        .description(safe(row.getExplanation()).isBlank() ? "(imported)" : safe(row.getExplanation()))
                                        .termCategory(cat)
                                        .build()
                        ));
            } else {
                t = termRepository.save(
                        Term.builder()
                                .title(title)
                                .description("(imported)")
                                .termCategory(null)
                                .build()
                );
            }

            return t.getId();
        });
    }

    // =============================
    // TopicTag attach (ID cache + getReference)
    // =============================

    private void attachTopicTagsToTerm(Term term, List<String> keys) {
        if (term == null || keys == null || keys.isEmpty()) return;

        Set<String> applied = appliedByTerm.computeIfAbsent(term.getId(), k -> new HashSet<>());

        List<String> toAdd = keys.stream()
                .filter(k -> !applied.contains(k))
                .toList();

        if (toAdd.isEmpty()) return;

        for (String key : toAdd) {
            Long tagId = topicTagIdCache.computeIfAbsent(key, kk ->
                    topicTagRepository.findByKey(kk)
                            .map(TopicTag::getId)
                            .orElseGet(() -> topicTagRepository.save(TopicTag.create(kk)).getId())
            );

            TopicTag tagRef = em.getReference(TopicTag.class, tagId);
            term.getTopicTags().add(tagRef);
            applied.add(key);
        }

        // 관계 저장
        termRepository.save(term);
    }

    // =============================
    // Question create
    // =============================

    protected void createOneQuestion(QuizSet set, QuizImportRow row) {
        QuestionType type = QuestionType.from(row.getQuestionType());

        TermCategory termCategory = resolveCategory(row.getTermCategoryId());

        // 1) termId 결정 (있으면 사용, 없으면 title로 생성 후 ID 확보)
        Long termId = row.getTermId();
        if (termId == null) {
            if (safe(row.getTermTitle()).isBlank()) {
                throw new IllegalArgumentException("term_id가 없으면 term_title은 필수입니다. question_key=" + row.getQuestionKey());
            }
            termId = resolveOrCreateTermId(row, termCategory);
        }

        if (termId == null) {
            throw new IllegalArgumentException("termId resolve 실패. question_key=" + row.getQuestionKey());
        }

        // 2) row 트랜잭션 안에서 reference로 붙여 사용
        Term term = em.getReference(Term.class, termId);

        // 3) term에 카테고리 없으면 채워두기 (getReference라 setter OK)
        if (termCategory != null && term.getTermCategory() == null) {
            term.setTermCategory(termCategory);
            termRepository.save(term);
        }

        // 4) 토픽태그 attach
        attachTopicTagsToTerm(term, parseTopicTagKeys(row.getTermTopicTagKeys()));

        // 5) questionText 정규화 + 중복 스킵
        String questionText = nvl(row.getQuestionText(), "(빈 문제)").trim();
        questionText = questionText.replaceAll("\\s+", " ");

        if (quizQuestionRepository.existsByQuizSet_IdAndQuestionTypeAndQuestionTextAndTerm_Id(
                set.getId(),
                type,
                questionText,
                termId
        )) {
            log.info("[QUIZ-IMPORT] duplicate skip. setId={}, termId={}, type={}, text={}",
                    set.getId(), termId, type, questionText);
            return;
        }

        String explanation = safe(row.getExplanation());

        // 6) 난이도 파싱
        DifficultyLevel difficulty = DifficultyLevel.MEDIUM;
        String diffStr = row.getDifficulty();
        if (diffStr != null && !diffStr.isBlank()) {
            try {
                difficulty = DifficultyLevel.valueOf(diffStr.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                log.warn("[QUIZ-IMPORT] invalid difficulty '{}', default MEDIUM", diffStr);
            }
        }

        // 7) 문제 저장
        QuizQuestion q = new QuizQuestion(
                term,
                termCategory,
                type,
                difficulty,
                questionText,
                set,
                explanation
        );
        quizQuestionRepository.save(q);

        // 8) 타입별 정답/보기 저장
        switch (type) {
            case OX -> createOxChoices(q, row);
            case CHOICE -> createChoiceChoices(q, row);
            case INITIALS -> createInitialsAnswer(q, row);
        }
    }

    private void createOxChoices(QuizQuestion q, QuizImportRow row) {
        String at = safe(row.getTextAnswer());
        int answerIdx = 1; // 기본 O

        if (!at.isBlank()) {
            if ("o".equalsIgnoreCase(at)) {
                answerIdx = 1;
            } else if ("x".equalsIgnoreCase(at)) {
                answerIdx = 2;
            }
        } else {
            if (Boolean.TRUE.equals(row.getChoice1IsAnswer())) {
                answerIdx = 1;
            } else if (Boolean.TRUE.equals(row.getChoice2IsAnswer())) {
                answerIdx = 2;
            }
        }

        quizChoiceRepository.saveAll(List.of(
                new QuizChoice(q, "O", answerIdx == 1),
                new QuizChoice(q, "X", answerIdx == 2)
        ));
    }

    private void createChoiceChoices(QuizQuestion q, QuizImportRow row) {
        List<String> texts = new ArrayList<>();
        List<Boolean> flags = new ArrayList<>();

        addOption(texts, flags, row.getChoice1Text(), row.getChoice1IsAnswer());
        addOption(texts, flags, row.getChoice2Text(), row.getChoice2IsAnswer());
        addOption(texts, flags, row.getChoice3Text(), row.getChoice3IsAnswer());
        addOption(texts, flags, row.getChoice4Text(), row.getChoice4IsAnswer());

        if (texts.size() < 2) {
            throw new IllegalArgumentException("CHOICE는 최소 2개 보기가 필요합니다.");
        }

        boolean hasCorrect = flags.stream().anyMatch(Boolean::booleanValue);

        if (!hasCorrect) {
            String at = safe(row.getTextAnswer());
            if (!at.isBlank()) {
                for (int i = 0; i < texts.size(); i++) {
                    if (texts.get(i).trim().equalsIgnoreCase(at)) {
                        flags.set(i, true);
                        hasCorrect = true;
                        break;
                    }
                }
            }
        }

        if (!hasCorrect) {
            flags.set(0, true);
        }

        for (int i = 0; i < texts.size(); i++) {
            quizChoiceRepository.save(new QuizChoice(q, texts.get(i), Boolean.TRUE.equals(flags.get(i))));
        }
    }

    private void createInitialsAnswer(QuizQuestion q, QuizImportRow row) {
        String at = safe(row.getTextAnswer());
        if (at.isBlank()) {
            throw new IllegalArgumentException("INITIALS는 text_answer(텍스트 정답)가 필요합니다.");
        }
        QuizTextAnswer textAnswer = QuizTextAnswer.create(q, at);
        quizTextAnswerRepository.save(textAnswer);
    }

    private void addOption(List<String> texts, List<Boolean> flags, String text, Boolean isAns) {
        text = safe(text);
        if (text.isBlank()) return;
        texts.add(text);
        flags.add(Boolean.TRUE.equals(isAns));
    }

    // =============================
    // Utils
    // =============================

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private List<String> parseTopicTagKeys(String raw) {
        if (raw == null) return List.of();
        String s = raw.trim();
        if (s.isEmpty()) return List.of();

        return Arrays.stream(s.split("[|,;]", -1))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(x -> !x.isBlank())
                .distinct()
                .toList();
    }
}