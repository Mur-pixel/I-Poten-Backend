package com.cygnus.ipoten.batch.quiz;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz_question.repository.QuizTextAnswerRepository;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term_category.repository.TermCategoryRepository;
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

    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizTextAnswerRepository quizTextAnswerRepository;

    private final TermCategoryRepository termCategoryRepository;
    private final TermRepository termRepository;

    private final TransactionTemplate txTemplate;
    private final EntityManager em;

    // row 단위 트랜잭션에서도 안전하게 쓰기 위해 "ID" 캐시
    private final Map<String, Long> termIdCache = new HashMap<>();

    @Override
    public void run(String... args) {
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
            importQuestions(quizPath);
        } catch (Exception e) {
            // 애플리케이션 부팅이 죽지 않게 보호
            log.error("[QUIZ-IMPORT] fatal error while importing questions from {}.", quizPath, e);
        }
    }

    private void importQuestions(String quizPath) throws Exception {
        termIdCache.clear();

        File file = new File(quizPath);
        if (!file.exists()) {
            log.error("[QUIZ-IMPORT] File not found: {}", quizPath);
            return;
        }

        log.info("[QUIZ-IMPORT] Importing QUESTION BANK from {}", quizPath);

        int lineNo = 0;
        int ok = 0, skip = 0, err = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("UTF-8")))) {
            String header = br.readLine(); // 첫 줄은 헤더
            lineNo++;

            if (header == null || header.isBlank()) {
                throw new IllegalArgumentException("헤더가 비어 있습니다. file=" + quizPath);
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
                    txTemplate.executeWithoutResult(status -> {
                        QuizImportRow row = parseRow(c, col);
                        createOneQuestion(row, currentLineNo);
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

    // =========================================================
    // Header parsing (TSV/CSV 자동 판별 + (필수)/(선택) 제거)
    // =========================================================

    private String detectDelimiterRegex(String headerLine) {
        int tabCount = headerLine.length() - headerLine.replace("\t", "").length();
        int commaCount = headerLine.length() - headerLine.replace(",", "").length();

        // 탭이 있으면 TSV 우선
        if (tabCount > 0 && tabCount >= commaCount) return "\\t";
        // 콤마가 많으면 CSV
        if (commaCount > 0) return ",";
        // 그 외는 탭으로 가정
        return "\\t";
    }

    private Map<String, Integer> buildHeaderIndex(String header, String delimRegex) {
        String[] heads = header.split(delimRegex, -1);

        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < heads.length; i++) {
            String key = normalizeHeaderKey(heads[i]);
            if (!key.isBlank()) {
                map.put(key, i);
            }
        }

        // 디버그: 지금 어떤 헤더로 읽혔는지 바로 보이게
        log.info("[QUIZ-IMPORT] header(raw)={}", header);
        log.info("[QUIZ-IMPORT] header(keys)={}", new TreeSet<>(map.keySet()));

        // 최소 컬럼 검증 (문제은행 시트 기준)
        require(map, "question_temp_key");
        require(map, "question_type");
        require(map, "difficulty");
        require(map, "question_text");

        return map;
    }

    private String normalizeHeaderKey(String raw) {
        if (raw == null) return "";
        String s = raw.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT); // BOM 제거 + 소문자

        // "question_temp_key (필수)" 같은 꼬리표 제거
        // (필수), (선택), (표시용) 등 뭐든 괄호로 끝나면 제거해버림
        s = s.replaceAll("\\s*\\(.*\\)\\s*$", "");

        // "term category id" 같은 케이스 대비: 공백 -> _
        s = s.replaceAll("\\s+", "_");

        return s;
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
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    // =========================================================
    // Row parsing (문제은행 TSV/CSV)
    // =========================================================

    private QuizImportRow parseRow(String[] c, Map<String, Integer> col) {

        String questionTempKey = get(c, col, "question_temp_key");

        String termTitle = get(c, col, "term_title");
        Long termId = parseLong(get(c, col, "term_id"));

        String termCategory = get(c, col, "term_category");
        Long termCategoryId = parseLong(get(c, col, "term_category_id"));

        String questionType = get(c, col, "question_type");
        String difficulty = get(c, col, "difficulty");
        String questionText = get(c, col, "question_text");
        String explanation = get(c, col, "explanation");

        String textAnswer = get(c, col, "text_answer");

        String choice1Text = get(c, col, "choice_1_text");
        Boolean choice1IsAnswer = parseBool(get(c, col, "choice_1_is_answer"));
        String choice2Text = get(c, col, "choice_2_text");
        Boolean choice2IsAnswer = parseBool(get(c, col, "choice_2_is_answer"));
        String choice3Text = get(c, col, "choice_3_text");
        Boolean choice3IsAnswer = parseBool(get(c, col, "choice_3_is_answer"));
        String choice4Text = get(c, col, "choice_4_text");
        Boolean choice4IsAnswer = parseBool(get(c, col, "choice_4_is_answer"));

        String label1Key = get(c, col, "label_1_key");
        String label2Key = get(c, col, "label_2_key");
        String label3Key = get(c, col, "label_3_key");
        String label4Key = get(c, col, "label_4_key");

        return new QuizImportRow(
                questionTempKey,
                termTitle, termId,
                termCategory, termCategoryId,
                questionType, difficulty, questionText, explanation,
                textAnswer,
                choice1Text, choice1IsAnswer,
                choice2Text, choice2IsAnswer,
                choice3Text, choice3IsAnswer,
                choice4Text, choice4IsAnswer,
                label1Key, label2Key, label3Key, label4Key
        );
    }

    // =========================================================
    // Term/Category resolve
    // =========================================================

    private TermCategory resolveCategory(QuizImportRow row) {
        Long termCategoryId = row.getTermCategoryId();
        if (termCategoryId == null) return null;

        return termCategoryRepository.findById(termCategoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "없는 term_category_id=" + termCategoryId +
                                " (question_temp_key=" + safe(row.getQuestionTempKey()) + ")"
                ));
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
                                .description(safe(row.getExplanation()).isBlank() ? "(imported)" : safe(row.getExplanation()))
                                .termCategory(null)
                                .build()
                );
            }

            return t.getId();
        });
    }

    // =========================================================
    // Question create (문제은행 적재)
    // =========================================================

    private void createOneQuestion(QuizImportRow row, int lineNo) {
        String qTempKey = safe(row.getQuestionTempKey());
        if (qTempKey.isBlank()) {
            throw new IllegalArgumentException("question_temp_key는 필수입니다. line=" + lineNo);
        }

        // 타입 파싱
        QuestionType type;
        try {
            type = QuestionType.from(row.getQuestionType());
        } catch (Exception e) {
            throw new IllegalArgumentException("question_type 파싱 실패: '" + safe(row.getQuestionType()) +
                    "' (question_temp_key=" + qTempKey + ")");
        }

        // 카테고리 resolve (없으면 null 허용)
        TermCategory termCategory = resolveCategory(row);

        // termId 결정 (있으면 사용, 없으면 title로 생성 후 ID 확보)
        Long termId = row.getTermId();
        if (termId == null) {
            if (safe(row.getTermTitle()).isBlank()) {
                throw new IllegalArgumentException("term_id가 없으면 term_title은 필수입니다. question_temp_key=" + qTempKey);
            }
            termId = resolveOrCreateTermId(row, termCategory);
        }

        if (termId == null) {
            throw new IllegalArgumentException("termId resolve 실패. question_temp_key=" + qTempKey);
        }

        Term term = em.getReference(Term.class, termId);

        // questionText 정규화 + 중복 스킵
        String questionText = nvl(row.getQuestionText(), "(빈 문제)").trim();
        questionText = questionText.replaceAll("\\s+", " ");

        if (quizQuestionRepository.existsByQuestionTypeAndQuestionTextAndTerm_Id(type, questionText, termId)) {
            log.info("[QUIZ-IMPORT] duplicate skip. termId={}, type={}, tempKey={}, text={}",
                    termId, type, qTempKey, questionText);
            return;
        }

        // 난이도 파싱
        DifficultyLevel difficulty = DifficultyLevel.MEDIUM;
        String diffStr = safe(row.getDifficulty());
        if (!diffStr.isBlank()) {
            try {
                difficulty = DifficultyLevel.valueOf(diffStr.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                log.warn("[QUIZ-IMPORT] invalid difficulty '{}', default MEDIUM. tempKey={}", diffStr, qTempKey);
            }
        }

        String explanation = safe(row.getExplanation());

        // 문제 저장
        QuizQuestion q = new QuizQuestion(
                term,
                termCategory,
                type,
                difficulty,
                questionText,
                explanation
        );
        quizQuestionRepository.save(q);

        // 타입별 정답/보기 저장
        switch (type) {
            case OX -> createOxChoices(q, row);
            case CHOICE -> createChoiceChoices(q, row);
            case INITIALS -> createTextAnswerRequired(q, row, type);
            default -> createTextAnswerOptional(q, row, type);
        }
    }

    private void createOxChoices(QuizQuestion q, QuizImportRow row) {
        // OX는 "text_answer"에 O/X 또는 TRUE/FALSE 가능, 또는 choice_is_answer로 지정 가능
        String at = safe(row.getTextAnswer());
        int answerIdx = 1; // 기본 O

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

    private void createChoiceChoices(QuizQuestion q, QuizImportRow row) {
        List<String> texts = new ArrayList<>();
        List<Boolean> flags = new ArrayList<>();

        addOption(texts, flags, row.getChoice1Text(), row.getChoice1IsAnswer());
        addOption(texts, flags, row.getChoice2Text(), row.getChoice2IsAnswer());
        addOption(texts, flags, row.getChoice3Text(), row.getChoice3IsAnswer());
        addOption(texts, flags, row.getChoice4Text(), row.getChoice4IsAnswer());

        if (texts.size() < 2) {
            throw new IllegalArgumentException("CHOICE는 최소 2개 보기가 필요합니다. question_temp_key=" + safe(row.getQuestionTempKey()));
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

        // 그래도 없으면 1번 정답 처리 (데이터 실수 보호)
        if (!hasCorrect) {
            flags.set(0, true);
        }

        for (int i = 0; i < texts.size(); i++) {
            quizChoiceRepository.save(new QuizChoice(q, texts.get(i), Boolean.TRUE.equals(flags.get(i))));
        }
    }

    private void createTextAnswerRequired(QuizQuestion q, QuizImportRow row, QuestionType type) {
        String at = safe(row.getTextAnswer());
        if (at.isBlank()) {
            throw new IllegalArgumentException(type + "는 text_answer(텍스트 정답)가 필요합니다. question_temp_key=" + safe(row.getQuestionTempKey()));
        }
        quizTextAnswerRepository.save(QuizTextAnswer.create(q, at));
    }

    private void createTextAnswerOptional(QuizQuestion q, QuizImportRow row, QuestionType type) {
        String at = safe(row.getTextAnswer());
        if (at.isBlank()) return;
        quizTextAnswerRepository.save(QuizTextAnswer.create(q, at));
    }

    private void addOption(List<String> texts, List<Boolean> flags, String text, Boolean isAns) {
        text = safe(text);
        if (text.isBlank()) return;
        texts.add(text);
        flags.add(Boolean.TRUE.equals(isAns));
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
