package com.cygnus.ipoten.quiz.service.generator;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_question.repository.QuizChoiceRepository;
import com.cygnus.ipoten.quiz.service.util.OptionQualityChecker;
import com.cygnus.ipoten.quiz.service.util.SeedUtil;
import com.cygnus.ipoten.term.entity.Term;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.cygnus.ipoten.quiz.service.util.OptionQualityChecker.repairOptions;

@Component
@RequiredArgsConstructor
public class AutoQuizGenerator {

    private final QuizChoiceRepository quizChoiceRepository;
    private final DifficultyProperties difficultyProperties;

    private final SeedUtil seedUtil = new SeedUtil();

    /** 1) 문항만 생성 (저장/보기 저장 안 함) */
    public List<QuizQuestion> generateQuestions(
            List<Term> terms,
            List<QuestionType> types,
            Integer count,
            Integer mcqEach,
            Integer oxEach,
            Integer initialsEach,
            SeedMode seedMode,
            Long accountId,         // DAILY 재현성용
            Long fixedSeed,         // FIXED 재현성용
            String difficulty
    ) {
        long seed = seedUtil.resolveSeed(seedMode, accountId, fixedSeed);
        Random rng = new Random(seed);
        DifficultyProperties.Profile profile = difficultyProperties.getProfileOrDefault(difficulty);

        List<Term> pool = shuffleBySeed(terms, rng);

        boolean initialsOnly = (types != null && !types.isEmpty()
                && types.stream().allMatch(t -> t == QuestionType.INITIALS));
        if (initialsOnly) {
            pool = pool.stream()
                    .filter(t -> !koreanCore(t.getTitle()).isEmpty())
                    .collect(Collectors.toList());
        }

        int target = (count != null && count > 0) ? count : calcByEach(pool, mcqEach, oxEach, initialsEach);

        List<QuizQuestion> out = new ArrayList<>();
        for (Term t : pool) {
            for (QuestionType type : types) {
                if (out.size() >= target) break;
                // INITIALS인데 한글 코어가 없으면 스킵 (다음 term로)
                if (type == QuestionType.INITIALS && koreanCore(t.getTitle()).isEmpty()) continue;

                QuizQuestion q = switch (type) {
                    case CHOICE   -> mkChoice(t, profile, rng);
                    case OX       -> mkOX(t, profile, rng);
                    case INITIALS -> mkInitials(t, profile);
                };
                if (q != null) out.add(q);
            }
            if (out.size() >= target) break;
        }
        return out;
    }

    /** 2) 저장된 문항들에 대해 보기 생성·저장 (이때는 q가 Managed) */
    public void createAndSaveChoicesFor(List<QuizQuestion> questions,
                                        SeedMode seedMode,
                                        Long accountId,
                                        Long fixedSeed) {
        long seed = seedUtil.resolveSeed(seedMode, accountId, fixedSeed);
        Random rng = new Random(seed);

        for (QuizQuestion q : questions) {
            DifficultyProperties.Profile profile = difficultyProperties.getProfileOrDefault("MEDIUM");

            if (q.getQuestionType() == QuestionType.OX) {
                var choices = List.of(
                        QuizChoice.create(q, "O", true),
                        QuizChoice.create(q, "X", false)
                );
                quizChoiceRepository.saveAll(choices);
                continue;
            }

            if (q.getQuestionType() == QuestionType.INITIALS) {
                continue;
            }

            // === 아래는 CHOICE 전용 ===
            String correct = normalize(q.getTerm().getDescription());
            List<String> distractors = pickDistractors(q.getTerm(), q.getQuestionType(), profile, rng);

            int optionCount = Math.max(2, profile.getOptionCount());
            List<String> options = new ArrayList<>(optionCount);
            options.add(correct);
            options.addAll(distractors.stream().limit(optionCount - 1).toList());

            options = repairOptions(
                    correct, options, toDifficulty(profile), 10,
                    (answer, currentOptions) -> genFallbackOption(currentOptions.size(), rng)
            );

            Collections.shuffle(options, rng);
            if (!acceptLengthBias(options, profile.getLengthBiasTolerance())) {
                Collections.shuffle(options, rng);
            }

            int answerIdx = -1;
            for (int i = 0; i < options.size(); i++) {
                boolean isAns = options.get(i).equals(correct);
                quizChoiceRepository.save(
                        QuizChoice.create(q, options.get(i), isAns)
                );
            }
        }
    }

    /** 최근 본 보기 텍스트(정규화) 재사용 배제 집합을 추가로 받는 버전 (JSAB-124) */
    public void createAndSaveChoicesForWithExclusion(List<QuizQuestion> questions,
                                                     SeedMode seedMode,
                                                     Long accountId,
                                                     Long fixedSeed,
                                                     Set<String> recentOptionNorms) {
        long seed = seedUtil.resolveSeed(seedMode, accountId, fixedSeed);
        Random rng = new Random(seed);

        for (QuizQuestion q : questions) {
            DifficultyProperties.Profile profile = difficultyProperties.getProfileOrDefault("MEDIUM");

            if (q.getQuestionType() == QuestionType.OX) {
                var choices = List.of(
                        QuizChoice.create(q, "O", true),
                        QuizChoice.create(q, "X", false)
                );
                quizChoiceRepository.saveAll(choices);
                continue;
            }

            if (q.getQuestionType() == QuestionType.INITIALS) {
                continue;
            }

            // === CHOICE 전용 ===
            String correct = (q.getTerm().getTitle() == null) ? "" : q.getTerm().getTitle().trim();
            List<String> distractors = pickDistractors(q.getTerm(), q.getQuestionType(), profile, rng);

            int optionCount = Math.max(2, profile.getOptionCount());
            List<String> options = new ArrayList<>(optionCount);
            options.add(correct);
            options.addAll(distractors.stream().limit(optionCount - 1).toList());

            if (recentOptionNorms != null && !recentOptionNorms.isEmpty()) {
                for (int i = 0; i < options.size(); i++) {
                    String opt = options.get(i);
                    if (opt == null) continue;
                    if (Objects.equals(opt, correct)) continue; // 정답은 보존
                    String norm = OptionQualityChecker.normalize(opt);
                    if (recentOptionNorms.contains(norm)) {
                        options.set(i, genFallbackOption(i, rng));
                    }
                }
            }

            final Set<String> recentSet = (recentOptionNorms == null ? Set.of() : recentOptionNorms);
            options = repairOptions(
                    correct, options, toDifficulty(profile), 10,
                    (answer, currentOptions) -> {
                        for (int tries = 0; tries < 10; tries++) {
                            String cand = genFallbackOption(currentOptions.size(), rng);
                            String norm = OptionQualityChecker.normalize(cand);
                            if (!recentSet.contains(norm)) return cand;
                        }
                        return genFallbackOption(currentOptions.size(), rng);
                    }
            );

            Collections.shuffle(options, rng);
            if (!acceptLengthBias(options, profile.getLengthBiasTolerance())) {
                Collections.shuffle(options, rng);
            }

            int answerIdx = -1;
            for (int i = 0; i < options.size(); i++) {
                boolean isAns = options.get(i).equals(correct);
                quizChoiceRepository.save(
                        QuizChoice.create(q, options.get(i), isAns)
                );
            }
        }
    }

    // --- 문항 템플릿(+난이도 지문 변형) ---
    private QuizQuestion mkChoice(Term t, DifficultyProperties.Profile p, Random rng) {
        String stem = normalize(t.getDescription());
        stem = maybeNegateOrReplace(stem, p, rng);
        return new QuizQuestion(
                t,
                t.getTermCategory(),
                QuestionType.CHOICE,
                stem,
                null
        );
    }


    private QuizQuestion mkOX(Term t, DifficultyProperties.Profile p, Random rng) {
        String base = "다음 설명은 '" + t.getTitle() + "'에 대한 올바른 설명이다.";
        base = maybeNegateOrReplace(base, p, rng);
        return new QuizQuestion(
                t,
                t.getTermCategory(),
                QuestionType.OX,
                base,
                null
        );
    }

    private QuizQuestion mkInitials(Term t, DifficultyProperties.Profile p) {
        String core = koreanCore(t.getTitle());
        if (core.isEmpty()) return null;

        String hint  = toChoseong(core);
        String brief = oneLine(t.getDescription(), 140);
        if (brief.isBlank()) brief = "~.";

        String stem = "초성 힌트: " + hint + "\n설명: " + brief;

        return new QuizQuestion(
                t,
                t.getTermCategory(),
                QuestionType.INITIALS,
                stem,
                null
        );
    }

    // --- 시드/셔플 유틸 ---
    private List<Term> shuffleBySeed(List<Term> terms, Random rng) {
        if (terms == null || terms.size() <= 1) return terms;
        List<Term> copy = new ArrayList<>(terms);
        Collections.shuffle(copy, rng);
        return copy;
    }

    private int calcByEach(List<Term> pool, Integer mcqEach, Integer oxEach, Integer initialsEach) {
        int e = (mcqEach == null ? 0 : mcqEach) + (oxEach == null ? 0 : oxEach) + (initialsEach == null ? 0 : initialsEach);
        return Math.max(1, e * pool.size());
    }

    // --- 텍스트/유사도/오답 선택 ---
    private String normalize(String s) {
        return (s == null || s.isBlank()) ? "" : (s.endsWith("다") ? s : s + "이다.");
    }

    private String maybeNegateOrReplace(String stem, DifficultyProperties.Profile p, Random rng) {
        String out = stem;
        if (rng.nextDouble() < p.getNegateProb()) {
            // 아주 단순한 부정화(예시): "올바른" → "올바르지 않은"
            out = out.replace("올바른", "올바르지 않은")
                    .replace("맞는", "맞지 않은");
        }
        if (rng.nextDouble() < p.getReplaceProb()) {
            // 단순 치환(예시): "다음 설명은" → "아래 진술은"
            out = out.replace("다음 설명은", "아래 진술은");
        }
        return out;
    }

    private int lengthNoSpaces(String s) {
        return (s == null) ? 0 : s.replace(" ", "").length();
    }

    private String toChoseong(String s) {
        if (s == null) return "";
        final char HANGUL_BASE = 0xAC00;
        final int CHOSUNG_INTERVAL = 21 * 28;
        final char[] CHO = {
                'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
        };
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch >= 0xAC00 && ch <= 0xD7A3) {
                int idx = (ch - HANGUL_BASE) / CHOSUNG_INTERVAL;
                sb.append(CHO[idx]);
            } else if (ch >= 0x3131 && ch <= 0x314E) {
                // 이미 자모면 그대로
                sb.append(ch);
            } // 그 외 문자는 무시(정책에 따라 공백 등 처리)
        }
        return sb.toString();
    }

    private List<String> pickDistractors(Term t, QuestionType type, DifficultyProperties.Profile p, Random rng) {
        // TODO: 실제 구현 — 같은 카테고리/태그에서 후보 수집
        // 일단 플레이스홀더 풀에서 유사도/개수 정책만 적용
        List<String> rawPool = List.of("오답1", "오답2", "오답3", "오답4", "오답5", "오답6", "오답7", "오답8");

        String answer = (type == QuestionType.INITIALS)
                ? toChoseong(t.getTitle())
                : normalize(t.getDescription());

        // 유사도 필터링(Jaccard on token set)
        List<String> filtered = rawPool.stream()
                .filter(s -> {
                    double sim = jaccard(tokenize(answer), tokenize(s));
                    return sim >= p.getSimilarityMin() && sim <= p.getSimilarityMax();
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // 부족하면 채우기(유사도 무시, 재현성 있게)
        if (filtered.size() < p.getMaxDistractors()) {
            for (String s : rawPool) {
                if (filtered.size() >= p.getMaxDistractors()) break;
                if (!filtered.contains(s)) filtered.add(s);
            }
        }

        // 시드 기반 셔플 후 리미트
        Collections.shuffle(filtered, rng);
        return filtered.stream().limit(p.getMaxDistractors()).toList();
    }

    private Set<String> tokenize(String s) {
        if (s == null) return Set.of();
        String[] arr = s.replaceAll("[^ㄱ-ㅎ가-힣a-zA-Z0-9 ]", " ")
                .toLowerCase(Locale.ROOT)
                .split("\\s+");
        return Arrays.stream(arr).filter(w -> !w.isBlank()).collect(Collectors.toSet());
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    private boolean acceptLengthBias(List<String> options, double tolerance) {
        if (options.isEmpty()) return true;
        int min = options.stream().mapToInt(String::length).min().orElse(0);
        int max = options.stream().mapToInt(String::length).max().orElse(0);
        if (max == 0) return true;
        double diffRatio = (double) (max - min) / max;
        return diffRatio <= Math.max(0, Math.min(1, tolerance));
    }

    private String genFallbackOption(int idx, Random rng) {
        String[] seeds = { "대체 보기", "유사 개념", "혼동 개념", "관련 용어", "비슷한 표현" };
        String base = seeds[Math.abs(rng.nextInt()) % seeds.length];
        return base + " " + (idx + 1);
    }

    private OptionQualityChecker.Difficulty toDifficulty(DifficultyProperties.Profile p) {
        // 필요 시 Profile -> Difficulty 매핑 고도화
        return OptionQualityChecker.Difficulty.MEDIUM;
    }

    private boolean hasHangul(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '\uAC00' && c <= '\uD7A3') || (c >= '\u3131' && c <= '\u318E')) return true;
        }
        return false;
    }

    private String koreanCore(String s) {
        if (s == null) return "";
        String t = s.trim();
        int p = t.indexOf('(');
        if (p >= 0) {
            String before = t.substring(0, p).trim();
            if (hasHangul(before)) return before;
            int q = t.indexOf(')', p + 1);
            if (q > p) {
                String inside = t.substring(p + 1, q).trim();
                if (hasHangul(inside)) return inside;
            }
        }
        return hasHangul(t) ? t : "";
    }

    private static String oneLine(String s, int maxLen) {
        if (s == null) return "";
        String t = s.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
        if (t.length() > maxLen) t = t.substring(0, Math.max(0, maxLen - 1)) + "…";
        return t;
    }
}
