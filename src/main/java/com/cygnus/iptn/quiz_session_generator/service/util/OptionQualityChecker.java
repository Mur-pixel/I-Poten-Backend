package com.cygnus.iptn.quiz_session_generator.service.util;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public final class OptionQualityChecker {

    private OptionQualityChecker() {}

    public enum Difficulty { EASY, MEDIUM, HARD }

    // 길이 하한(공백/기호 제거 후, "문자 수")
    public static int minLenByDifficulty(Difficulty d) {
        return switch (d) {
            case EASY, MEDIUM -> 2;
            case HARD -> 3;
        };
    }

    // 공백/비문자 처리
    private static final Pattern WHITES = Pattern.compile("\\s+");
    // "문자(모든 언어), 숫자" 외는 공백으로 치환 → 토큰 경계만 유지
    private static final Pattern NON_LETTER_DIGIT = Pattern.compile("[^\\p{L}\\p{Nd} ]");
    // 길이 계산/스쿼시용: 문자/숫자 외 제거
    private static final Pattern NON_WORDS = Pattern.compile("[^\\p{L}\\p{Nd}]");
    private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

    /** 정규화: NFKD, 소문자, 비문자(문자/숫자 외) → 공백 치환, 공백 압축 */
    public static String normalize(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFKD)
                .toLowerCase(Locale.ROOT);
        t = NON_LETTER_DIGIT.matcher(t).replaceAll(" ");
        t = WHITES.matcher(t).replaceAll(" ").trim();
        return t;
    }

    /** 정규화 + 공백 제거(동등성/근접성 강판정용) */
    private static String normalizeSquash(String s) {
        String t = normalize(s);
        // 토큰 간 공백까지 제거해 "a b c"와 "abc"를 동일하게 만듦
        return t.replace(" ", "");
    }

    /** “의미 없는 보기” 판정 */
    public static boolean isMeaningless(String raw) {
        if (raw == null || raw.isBlank()) return true;
        String s = normalize(raw);
        if (s.isEmpty()) return true;                 // 전부 제거됨
        if (s.length() == 1) return true;             // 단문자
        if (DIGITS_ONLY.matcher(s).matches()) return true; // 숫자만
        return false;
    }

    /** 길이 기준(문자/숫자만 카운트) */
    public static boolean isTooShort(String raw, Difficulty d) {
        String core = NON_WORDS.matcher(normalize(raw)).replaceAll("");
        return core.codePointCount(0, core.length()) < minLenByDifficulty(d);
    }

    /** Jaccard 유사도(토큰 집합) */
    public static double jaccardTokens(String a, String b) {
        Set<String> A = new HashSet<>(List.of(splitTokens(normalize(a))));
        Set<String> B = new HashSet<>(List.of(splitTokens(normalize(b))));
        A.remove(""); B.remove("");
        if (A.isEmpty() && B.isEmpty()) return 1.0;
        Set<String> inter = new HashSet<>(A); inter.retainAll(B);
        Set<String> union = new HashSet<>(A); union.addAll(B);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    private static String[] splitTokens(String s) {
        if (s.isEmpty()) return new String[0];
        return s.split("\\s+");
    }

    /** 단순 Levenshtein (짧은 문자열 비교용) — normalize 후 비교 */
    public static int levenshtein(String a, String b) {
        String x = normalize(a), y = normalize(b);
        int n = x.length(), m = y.length();
        int[][] dp = new int[n+1][m+1];
        for (int i=0;i<=n;i++) dp[i][0]=i;
        for (int j=0;j<=m;j++) dp[0][j]=j;
        for (int i=1;i<=n;i++) {
            for (int j=1;j<=m;j++) {
                int cost = (x.charAt(i-1)==y.charAt(j-1))?0:1;
                dp[i][j] = Math.min(
                        Math.min(dp[i-1][j]+1, dp[i][j-1]+1),
                        dp[i-1][j-1]+cost
                );
            }
        }
        return dp[n][m];
    }

    /** 중복/부분중복 판정 강화:
     *  1) squash 동등(공백 무시)이면 즉시 true
     *  2) Jaccard 토큰 ≥ 0.8
     *  3) (둘 다 ≤10자) Levenshtein ≤ 1
     */
    public static boolean nearDuplicate(String a, String b) {
        // (1) 공백 무시 동등성
        if (normalizeSquash(a).equals(normalizeSquash(b))) return true;

        // (2) 토큰 유사도
        double jac = jaccardTokens(a, b);
        if (jac >= 0.8) return true;

        // (3) 짧은 문자열 근접성
        String na = normalize(a), nb = normalize(b);
        if (na.length() <= 10 && nb.length() <= 10) {
            return levenshtein(na, nb) <= 1;
        }
        return false;
    }

    /** 정답과 정규화 동일 여부(공백 무시 동등성 사용) */
    public static boolean sameAsAnswer(String option, String answer) {
        return normalizeSquash(option).equals(normalizeSquash(answer));
    }

    /** 옵션 세트 품질 보정
     *  - 의미 없음/너무 짧음/정답 동일 → 재생성(실패 시 강제 fallback)
     *  - 오답끼리 중복/부분중복 제거 → 재생성(실패 시 강제 fallback)
     */
    public static List<String> repairOptions(
            String answer,
            List<String> options,               // size == optionCount, answer 포함
            Difficulty difficulty,
            int maxRegenerateTrials,
            DistractorSupplier supplier          // 오답 재생성기
    ) {
        int n = options.size();
        List<String> fixed = new ArrayList<>(options);

        // 1) 의미·길이·정답동일 필터 → 교체(실패 시 강제 fallback)
        for (int i = 0; i < n; i++) {
            if (Objects.equals(fixed.get(i), answer)) continue;
            int trials = 0;
            boolean ok = false;
            while (trials++ <= maxRegenerateTrials) {
                String cur = fixed.get(i);
                if (!isMeaningless(cur)
                        && !isTooShort(cur, difficulty)
                        && !sameAsAnswer(cur, answer)) {
                    ok = true; break;
                }
                fixed.set(i, supplier.regenerate(answer, fixed));
            }
            if (!ok) {
                fixed.set(i, forceDistinctFallback(answer, fixed, difficulty, i));
            }
        }

        // 2) 오답끼리 중복 제거 → 필요 시 반복 교정(실패 시 강제 fallback)
        boolean changed;
        int guard = 0;
        do {
            changed = false;
            guard++;
            outer:
            for (int i = 0; i < n; i++) {
                if (sameAsAnswer(fixed.get(i), answer)) continue;
                for (int j = i+1; j < n; j++) {
                    if (sameAsAnswer(fixed.get(j), answer)) continue;
                    if (nearDuplicate(fixed.get(i), fixed.get(j))) {
                        int trials = 0;
                        boolean replaced = false;
                        while (trials++ <= maxRegenerateTrials) {
                            String candidate = supplier.regenerate(answer, fixed);
                            if (!isMeaningless(candidate)
                                    && !isTooShort(candidate, difficulty)
                                    && !sameAsAnswer(candidate, answer)
                                    && fixed.stream().noneMatch(x -> nearDuplicate(x, candidate))) {
                                fixed.set(j, candidate);
                                changed = true;
                                replaced = true;
                                break;
                            }
                        }
                        if (!replaced) {
                            fixed.set(j, forceDistinctFallback(answer, fixed, difficulty, j));
                            changed = true;
                        }
                        if (changed) break outer;
                    }
                }
            }
        } while (changed && guard <= n * 3); // 과도 루프 방지

        return fixed;
    }

    /** 강제 대체값 생성: 의미/길이/중복/정답동일을 모두 피하는 안전한 문자열 */
    private static String forceDistinctFallback(String answer, List<String> current, Difficulty d, int serialHint) {
        int serial = Math.max(1, serialHint + 1);
        // 길이 하한과 의미 기준을 통과하는 한글/영문 조합 베이스
        while (true) {
            String candidate = "보기옵션" + serial; // 한글 4자 + 숫자 → 최소 2~3자 기준 충족
            serial++;

            if (isMeaningless(candidate)) continue;
            if (isTooShort(candidate, d)) continue;
            if (sameAsAnswer(candidate, answer)) continue;

            boolean dup = false;
            for (String x : current) {
                if (nearDuplicate(x, candidate)) { dup = true; break; }
            }
            if (!dup) return candidate;
        }
    }

    /** 오답 재생성기 인터페이스 (DB/모델/사전 등 구현체 주입) */
    public interface DistractorSupplier {
        String regenerate(String answer, List<String> currentOptions);
    }
}
