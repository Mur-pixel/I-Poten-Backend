package com.cygnus.iptn.batch.quiz;

import com.cygnus.iptn.quiz_question.entity.QuizChoice;
import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import com.cygnus.iptn.quiz_question.entity.QuizTextAnswer;
import com.cygnus.iptn.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.iptn.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.iptn.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.iptn.quiz_set.entity.QuizSet;
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
public class QuizSetTsvImportRunner implements CommandLineRunner {

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;

    private final TransactionTemplate txTemplate;
    private final EntityManager em;

    // set_title -> setId 캐시
    private final Map<String, Long> setIdCache = new HashMap<>();
    // set_title -> next orderNo (order_no 없을 때 자동 증가)
    private final Map<String, Integer> orderSeq = new HashMap<>();

    @Override
    public void run(String... args) {
        String setPath = null;
        for (String arg : args) {
            if (arg.startsWith("--set=")) {
                setPath = arg.substring("--set=".length());
            }
        }
        if (setPath == null) {
            log.info("[QUIZ-SET-IMPORT] skipped (no --set=...)");
            return;
        }

        try {
            importSet(setPath);
        } catch (Exception e) {
            log.error("[QUIZ-SET-IMPORT] fatal error while importing set from {}.", setPath, e);
        }
    }

    private void importSet(String setPath) throws Exception {
        setIdCache.clear();
        orderSeq.clear();

        File file = new File(setPath);
        if (!file.exists()) {
            log.error("[QUIZ-SET-IMPORT] File not found: {}", setPath);
            return;
        }

        log.info("[QUIZ-SET-IMPORT] Importing QUIZ SET rows from {}", setPath);

        int lineNo = 0;
        int ok = 0, skip = 0, err = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("UTF-8")))) {
            String header = br.readLine();
            lineNo++;

            if (header == null || header.isBlank()) {
                throw new IllegalArgumentException("헤더가 비어 있습니다. file=" + setPath);
            }

            String delimRegex = detectDelimiterRegex(header);
            Map<String, Integer> col = buildHeaderIndex(header, delimRegex);

            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    skip++;
                    continue;
                }

                String[] c = line.split(delimRegex, -1);

                try {
                    final int currentLineNo = lineNo;

                    Boolean created = txTemplate.execute(status -> {
                        QuizSetImportRow row = parseRow(c, col);

                        // (필수) set_title / question_temp_key / question_type / difficulty / question_text
                        if (safe(row.getSetTitle()).isBlank()) {
                            log.warn("[QUIZ-SET-IMPORT] skip: empty set_title. line={}", currentLineNo);
                            return false;
                        }
                        if (safe(row.getQuestionTempKey()).isBlank()) {
                            log.warn("[QUIZ-SET-IMPORT] skip: empty question_temp_key. set_title={}, line={}",
                                    safe(row.getSetTitle()), currentLineNo);
                            return false;
                        }

                        return createAndAttachOne(row, currentLineNo);
                    });

                    if (Boolean.TRUE.equals(created)) ok++;
                    else skip++;

                } catch (Exception ex) {
                    err++;
                    log.warn("[QUIZ-SET-IMPORT] line {} failed: {}", lineNo, ex.getMessage(), ex);
                    try { em.clear(); } catch (Exception ignore) {}
                }
            }
        }

        log.info("[QUIZ-SET-IMPORT] done. ok={}, skip={}, err={}", ok, skip, err);
    }

    // =============================
    // Core: create question + attach to set
    // =============================

    /**
     * @return true=생성/링크 성공, false=스킵(중복/데이터 부족 등)
     */
    private boolean createAndAttachOne(QuizSetImportRow row, int lineNo) {
        String setTitle = safe(row.getSetTitle());
        Long setId = resolveSetIdByTitle(setTitle);

        QuestionType type = resolveQuestionType(row, lineNo);
        if (type == null) return false;

        DifficultyLevel difficulty = resolveDifficulty(row);

        String questionText = safe(row.getQuestionText());
        if (questionText.isBlank()) {
            log.warn("[QUIZ-SET-IMPORT] skip: empty question_text. set_title={}, tempKey={}, line={}",
                    setTitle, safe(row.getQuestionTempKey()), lineNo);
            return false;
        }
        questionText = questionText.replaceAll("\\s+", " ");

        // 세트 내 중복(타입+난이도+문제텍스트)이면 스킵
        if (existsSameQuestionInSet(setId, type, difficulty, questionText)) {
            log.info("[QUIZ-SET-IMPORT] duplicate-in-set skip. setId={}, type={}, dl={}, tempKey={}",
                    setId, type, difficulty, safe(row.getQuestionTempKey()));
            return false;
        }

        // QuizQuestion 생성 (세트용: term/termCategory 없이 null로 생성)
        QuizQuestion q = new QuizQuestion(
                null,                 // term
                null,                 // termCategory
                type,
                difficulty,
                questionText,
                safe(row.getExplanation())
        );
        quizQuestionRepository.save(q);

        // 타입별 정답/보기 저장
        switch (type) {
            case OX -> createOxChoices(q, row);
            case CHOICE -> createChoiceChoices(q, row);
            default -> createTextAnswerByType(q, row, type);
        }

        // order_no 결정
        int orderNo = resolveOrderNo(row, setTitle);

        // link insert (quiz_set_question)
        insertSetLink(setId, q.getId(), orderNo);

        return true;
    }

    private QuestionType resolveQuestionType(QuizSetImportRow row, int lineNo) {
        String rawType = safe(row.getQuestionType());
        if (!rawType.isBlank()) {
            try {
                return QuestionType.from(rawType);
            } catch (Exception e) {
                throw new IllegalArgumentException("question_type 파싱 실패: '" + rawType +
                        "' (question_temp_key=" + safe(row.getQuestionTempKey()) + ", line=" + lineNo + ")");
            }
        }

        // 필수라고 했지만, 혹시 빈 값 섞여도 터지지 않게 “추론”
        boolean hasChoices =
                hasText(row.getChoice1Text()) || hasText(row.getChoice2Text()) ||
                        hasText(row.getChoice3Text()) || hasText(row.getChoice4Text());

        String at = safe(row.getTextAnswer()).toLowerCase(Locale.ROOT);

        if (hasChoices) {
            log.warn("[QUIZ-SET-IMPORT] infer type=CHOICE (empty question_type). line={}, tempKey={}",
                    lineNo, safe(row.getQuestionTempKey()));
            return QuestionType.CHOICE;
        }

        if (!at.isBlank()) {
            if (at.equals("o") || at.equals("x") || at.equals("true") || at.equals("false")) {
                log.warn("[QUIZ-SET-IMPORT] infer type=OX (empty question_type). line={}, tempKey={}",
                        lineNo, safe(row.getQuestionTempKey()));
                return QuestionType.OX;
            }
            // 텍스트 정답이 있으면 텍스트형으로 처리
            log.warn("[QUIZ-SET-IMPORT] infer type=INITIALS (empty question_type). line={}, tempKey={}",
                    lineNo, safe(row.getQuestionTempKey()));
            return QuestionType.INITIALS;
        }

        log.warn("[QUIZ-SET-IMPORT] skip: empty question_type and cannot infer. line={}, tempKey={}",
                lineNo, safe(row.getQuestionTempKey()));
        return null;
    }

    private DifficultyLevel resolveDifficulty(QuizSetImportRow row) {
        DifficultyLevel dl = DifficultyLevel.MEDIUM;
        String diffStr = safe(row.getDifficulty());
        if (!diffStr.isBlank()) {
            try {
                dl = DifficultyLevel.valueOf(diffStr.toUpperCase(Locale.ROOT));
            } catch (Exception ignore) {
                log.warn("[QUIZ-SET-IMPORT] invalid difficulty '{}', default MEDIUM. tempKey={}",
                        diffStr, safe(row.getQuestionTempKey()));
            }
        }
        return dl;
    }

    private int resolveOrderNo(QuizSetImportRow row, String setTitle) {
        Integer v = row.getOrderNo();
        if (v != null && v > 0) return v;

        int next = orderSeq.getOrDefault(setTitle, 0) + 1;
        orderSeq.put(setTitle, next);
        return next;
    }

    private boolean existsSameQuestionInSet(Long setId, QuestionType type, DifficultyLevel dl, String questionText) {
        // JPQL로 세트 내 중복 체크 (QuizSetQuestion 엔티티명이 그대로라는 가정)
        Long cnt = em.createQuery("""
                select count(q.id)
                from QuizSetQuestion link
                join link.quizQuestion q
                where link.quizSet.id = :setId
                  and q.questionType = :type
                  and q.difficulty = :dl
                  and q.questionText = :text
                """, Long.class)
                .setParameter("setId", setId)
                .setParameter("type", type)
                .setParameter("dl", dl)
                .setParameter("text", questionText)
                .getSingleResult();

        return cnt != null && cnt > 0;
    }

    private void insertSetLink(Long setId, Long questionId, int orderNo) {
        // 링크 중복 방지
        Number exist = (Number) em.createNativeQuery("""
                select count(1)
                from quiz_set_question
                where quiz_set_id = ? and quiz_question_id = ?
                """)
                .setParameter(1, setId)
                .setParameter(2, questionId)
                .getSingleResult();

        if (exist != null && exist.longValue() > 0) return;

        em.createNativeQuery("""
                insert into quiz_set_question (quiz_set_id, quiz_question_id, order_no)
                values (?,?,?)
                """)
                .setParameter(1, setId)
                .setParameter(2, questionId)
                .setParameter(3, orderNo)
                .executeUpdate();
    }

    private Long resolveSetIdByTitle(String setTitle) {
        return setIdCache.computeIfAbsent(setTitle, t -> {
            List<QuizSet> found = em.createQuery("""
                    select s
                    from QuizSet s
                    where s.title = :title
                    order by s.id desc
                    """, QuizSet.class)
                    .setParameter("title", t)
                    .setMaxResults(1)
                    .getResultList();

            if (found.isEmpty()) {
                throw new IllegalArgumentException("QuizSet을 찾을 수 없습니다. set_title='" + t + "' (먼저 세트를 생성하세요)");
            }
            return found.get(0).getId();
        });
    }

    // =============================
    // Answer/Choices
    // =============================

    private void createOxChoices(QuizQuestion q, QuizSetImportRow row) {
        String at = safe(row.getTextAnswer());
        int answerIdx = 1; // default O

        if (!at.isBlank()) {
            if ("o".equalsIgnoreCase(at) || "true".equalsIgnoreCase(at)) answerIdx = 1;
            else if ("x".equalsIgnoreCase(at) || "false".equalsIgnoreCase(at)) answerIdx = 2;
        } else {
            if (Boolean.TRUE.equals(row.getChoice1IsAnswer())) answerIdx = 1;
            else if (Boolean.TRUE.equals(row.getChoice2IsAnswer())) answerIdx = 2;
        }

        quizChoiceRepository.saveAll(List.of(
                new QuizChoice(q, "O", answerIdx == 1),
                new QuizChoice(q, "X", answerIdx == 2)
        ));
    }

    private void createChoiceChoices(QuizQuestion q, QuizSetImportRow row) {
        List<String> texts = new ArrayList<>();
        List<Boolean> flags = new ArrayList<>();

        addOption(texts, flags, row.getChoice1Text(), row.getChoice1IsAnswer());
        addOption(texts, flags, row.getChoice2Text(), row.getChoice2IsAnswer());
        addOption(texts, flags, row.getChoice3Text(), row.getChoice3IsAnswer());
        addOption(texts, flags, row.getChoice4Text(), row.getChoice4IsAnswer());

        if (texts.size() < 2) {
            throw new IllegalArgumentException("CHOICE는 최소 2개 보기가 필요합니다. tempKey=" + safe(row.getQuestionTempKey()));
        }

        boolean hasCorrect = flags.stream().anyMatch(Boolean::booleanValue);

        // 정답 플래그가 하나도 없으면 text_answer로 매칭
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

        // 그래도 없으면 1번을 정답 처리
        if (!hasCorrect) flags.set(0, true);

        for (int i = 0; i < texts.size(); i++) {
            quizChoiceRepository.save(new QuizChoice(q, texts.get(i), Boolean.TRUE.equals(flags.get(i))));
        }
    }

    private void createTextAnswerByType(QuizQuestion q, QuizSetImportRow row, QuestionType type) {
        String at = safe(row.getTextAnswer());

        // “TEXT일 때 필수”라고 했으니: 타입명이 TEXT 포함이거나 INITIALS면 필수 취급
        boolean required = type == QuestionType.INITIALS || type.name().contains("TEXT");

        if (required && at.isBlank()) {
            throw new IllegalArgumentException(type + "는 text_answer가 필요합니다. tempKey=" + safe(row.getQuestionTempKey()));
        }
        if (at.isBlank()) return;

        quizTextAnswerRepository.save(QuizTextAnswer.create(q, at));
    }

    private void addOption(List<String> texts, List<Boolean> flags, String text, Boolean isAns) {
        text = safe(text);
        if (text.isBlank()) return;
        texts.add(text);
        flags.add(Boolean.TRUE.equals(isAns));
    }

    // =============================
    // Parsing utils (header/row)
    // =============================

    private String detectDelimiterRegex(String headerLine) {
        int tabCount = headerLine.length() - headerLine.replace("\t", "").length();
        int commaCount = headerLine.length() - headerLine.replace(",", "").length();
        if (tabCount > 0 && tabCount >= commaCount) return "\\t";
        if (commaCount > 0) return ",";
        return "\\t";
    }

    private Map<String, Integer> buildHeaderIndex(String header, String delimRegex) {
        String[] heads = header.split(delimRegex, -1);

        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < heads.length; i++) {
            String key = normalizeHeaderKey(heads[i]);
            if (!key.isBlank()) map.put(key, i);
        }

        log.info("[QUIZ-SET-IMPORT] header(raw)={}", header);
        log.info("[QUIZ-SET-IMPORT] header(keys)={}", new TreeSet<>(map.keySet()));

        require(map, "set_title");
        require(map, "question_temp_key");
        require(map, "question_type");
        require(map, "difficulty");
        require(map, "question_text");

        return map;
    }

    private String normalizeHeaderKey(String raw) {
        if (raw == null) return "";
        String s = raw.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("\\s*\\(.*\\)\\s*$", ""); // (필수)/(선택) 제거
        s = s.replaceAll("\\s+", "_");
        return s;
    }

    private void require(Map<String, Integer> map, String key) {
        if (!map.containsKey(key)) throw new IllegalArgumentException("헤더 누락: " + key);
    }

    private String get(String[] c, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        if (idx == null || idx < 0 || idx >= c.length) return "";
        return c[idx] == null ? "" : c[idx].trim();
    }

    private Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return null; }
    }

    private Boolean parseBool(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().toLowerCase(Locale.ROOT);
        return ("true".equals(v) || "1".equals(v) || "y".equals(v) || "yes".equals(v));
    }

    private QuizSetImportRow parseRow(String[] c, Map<String, Integer> col) {
        return new QuizSetImportRow(
                get(c, col, "set_title"),
                get(c, col, "question_temp_key"),
                parseInt(get(c, col, "order_no")),
                get(c, col, "set_type"),
                get(c, col, "question_type"),
                get(c, col, "difficulty"),
                get(c, col, "question_text"),
                get(c, col, "explanation"),
                get(c, col, "text_answer"),
                get(c, col, "choice_1_text"),
                parseBool(get(c, col, "choice_1_is_answer")),
                get(c, col, "choice_2_text"),
                parseBool(get(c, col, "choice_2_is_answer")),
                get(c, col, "choice_3_text"),
                parseBool(get(c, col, "choice_3_is_answer")),
                get(c, col, "choice_4_text"),
                parseBool(get(c, col, "choice_4_is_answer"))
        );
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isBlank();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
