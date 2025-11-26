package com.cygnus.ipoten.batch.quiz;

import com.cygnus.ipoten.quiz.entity.QuizChoice;
import com.cygnus.ipoten.quiz.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.QuizSet;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz.repository.QuizQuestionRepository;
import com.cygnus.ipoten.quiz.repository.QuizSetRepository;
import com.cygnus.ipoten.term.entity.Category;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.CategoryRepository;
import com.cygnus.ipoten.term.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final CategoryRepository categoryRepository;
    private final TermRepository termRepository; // 없으면 주석처리하고 term 기능 빼도 됩니다.

    @Override
    @Transactional
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

        File file = new File(quizPath);
        if (!file.exists()) {
            log.error("[QUIZ-IMPORT] File not found: {}", quizPath);
            return;
        }

        log.info("[QUIZ-IMPORT] Importing quizzes from {}", quizPath);

        int lineNo = 0;
        int ok = 0, skip = 0, err = 0;

        // 세트 캐시: 같은 set_title은 한 번만 생성
        Map<String, QuizSet> setCache = new LinkedHashMap<>();

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
                    QuizImportRow row = parseRow(c, col);

                    // 세트 확보/캐시
                    QuizSet set = setCache.computeIfAbsent(keyForSet(row), k -> {
                        QuizSet qs = new QuizSet(
                                nvl(row.getSetTitle(), "퀴즈 세트"),
                                resolveCategory(row.getCategoryId()),
                                Boolean.TRUE.equals(row.getSetRandom())
                        );
                        return quizSetRepository.save(qs);
                    });

                    // 질문 생성
                    createOneQuestion(set, row);
                    ok++;

                } catch (Exception ex) {
                    err++;
                    log.warn("[QUIZ-IMPORT] line {}: {}", lineNo, ex.getMessage());
                }
            }
        }

        log.info("[QUIZ-IMPORT] done. ok={}, skip={}, err={}", ok, skip, err);
    }

    private Map<String, Integer> buildHeaderIndex(String header) {
        if (header == null) throw new IllegalArgumentException("헤더가 비어 있습니다.");
        String[] heads = header.split("\t");
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < heads.length; i++) {
            map.put(heads[i].trim().toLowerCase(Locale.ROOT), i);
        }
        // 최소 컬럼 검증
        require(map, "set_title");
        require(map, "question_type");
        require(map, "question_text");
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

    private QuizImportRow parseRow(String[] c, Map<String, Integer> col) {
        String setTitle      = get(c, col, "set_title");
        Boolean setRandom    = parseBool(get(c, col, "set_random"));
        Long categoryId      = parseLong(get(c, col, "category_id"));
        Long termId          = parseLong(get(c, col, "term_id"));

        String questionType  = get(c, col, "question_type");
        String questionText  = get(c, col, "question_text");

        Integer answerIndex  = parseInt(get(c, col, "answer_index"));
        String answerText    = get(c, col, "answer_text");

        String choice1       = get(c, col, "choice1");
        String choice2       = get(c, col, "choice2");
        String choice3       = get(c, col, "choice3");
        String choice4       = get(c, col, "choice4");

        String explanation   = get(c, col, "explanation");
        Integer orderIndex   = parseInt(get(c, col, "order_index"));

        return new QuizImportRow(
                setTitle, setRandom, categoryId, termId,
                questionType, questionText,
                answerIndex, answerText,
                choice1, choice2, choice3, choice4,
                explanation, orderIndex
        );
    }

    private Boolean parseBool(String s) {
        if (s == null || s.isBlank()) return null;
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        return Integer.valueOf(s);
    }

    private Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        return Long.valueOf(s);
    }

    private String nvl(String s, String def) { return (s == null || s.isBlank()) ? def : s; }

    private String keyForSet(QuizImportRow r) {
        // 제목 + 카테고리 + 랜덤여부 조합으로 캐시 키
        return nvl(r.getSetTitle(), "") + "|" + nvl(String.valueOf(r.getCategoryId()), "") + "|" + String.valueOf(Boolean.TRUE.equals(r.getSetRandom()));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId).orElse(null);
    }

    private Term resolveTerm(Long termId) {
        if (termId == null) return null;
        return termRepository.findById(termId).orElse(null);
    }

    @Transactional
    protected void createOneQuestion(QuizSet set, QuizImportRow row) {
        QuestionType type = QuestionType.from(row.getQuestionType());
        Term term = resolveTerm(row.getTermId());
        Category category = resolveCategory(row.getCategoryId());

        // 공통: Question 생성 (answerIndex/answerText는 타입별로 세팅)
        QuizQuestion q = new QuizQuestion(
                term,
                category,
                type,
                nvl(row.getQuestionText(), "(빈 문제)"),
                set
        );

        if (row.getOrderIndex() != null) q.setOrderIndex(row.getOrderIndex());
        q.setRandom(Boolean.TRUE.equals(row.getSetRandom()));
        quizQuestionRepository.save(q); // PK 확보

        switch (type) {
            case OX -> {
                String ax = safe(row.getAnswerText());
                Integer ai = row.getAnswerIndex();
                int answerIdx = (ai != null) ? ai
                        : ("O".equalsIgnoreCase(ax) ? 1 : "X".equalsIgnoreCase(ax) ? 2 : 1);

                quizChoiceRepository.saveAll(List.of(
                        new QuizChoice(q, "O", answerIdx == 1, nvl(row.getExplanation(), "")),
                        new QuizChoice(q, "X", answerIdx == 2, nvl(row.getExplanation(), ""))
                ));
            }
            case CHOICE -> {
                List<String> opts = new ArrayList<>();
                for (String s : new String[]{ row.getChoice1(), row.getChoice2(), row.getChoice3(), row.getChoice4() }) {
                    s = safe(s);
                    if (!s.isBlank()) opts.add(s);
                }
                if (opts.size() < 2) throw new IllegalArgumentException("CHOICE는 최소 2개 보기가 필요합니다.");

                Integer ai = row.getAnswerIndex();
                if (ai == null || ai < 1 || ai > opts.size()) {
                    String at = safe(row.getAnswerText());
                    if (!at.isBlank()) {
                        int found = -1;
                        for (int i = 0; i < opts.size(); i++) {
                            if (opts.get(i).trim().equalsIgnoreCase(at)) { found = i + 1; break; }
                        }
                        if (found > 0) ai = found;
                    }
                }
                if (ai == null || ai < 1 || ai > opts.size()) {
                    throw new IllegalArgumentException("CHOICE 정답 인덱스가 유효하지 않습니다.");
                }

                for (int i = 0; i < opts.size(); i++) {
                    boolean isAns = (i + 1) == ai;
                    quizChoiceRepository.save(
                            new QuizChoice(q, opts.get(i), isAns, isAns ? nvl(row.getExplanation(), "") : null)
                    );
                }
            }
            case INITIALS -> {
                String at = safe(row.getAnswerText());
                if (at.isBlank()) throw new IllegalArgumentException("INITIALS는 answer_text(초성)가 필요합니다.");
                q.setAnswerText(at);
                quizQuestionRepository.save(q);
            }
        }
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}
