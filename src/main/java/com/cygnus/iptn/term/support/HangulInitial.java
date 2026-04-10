package com.cygnus.iptn.term.support;

import java.util.Map;

public class HangulInitial {
    private HangulInitial() {}

    private static final int SBASE = 0xAC00; // '가'
    private static final int LCOUNT = 19; // 초성 자음의 개수
    private static final int VCOUNT = 21; // 중성 모음의 개수
    private static final int TCOUNT = 28; // 종성 자음의 개수 (받침 없는 경우 포함)
    private static final int NCOUNT = VCOUNT * TCOUNT;  // 하나의 초성당 가능한 중성+종성의 조합 수 (588)
    private static final int SCOUNT = LCOUNT * NCOUNT;  // 전체 한글 음절 개수 (11172)

    // 초성 + LIndex (쌍자음은 이미 normalizae 단계에서 단일로 매핑됨)
    private static final Map<String,Integer> LINDEX = Map.ofEntries(
            Map.entry("ㄱ", 0),  Map.entry("ㄴ", 2),  Map.entry("ㄷ", 3), // ㄱ=0, ㄴ=2, ㄷ=3으로 매핑
            Map.entry("ㄹ", 5),  Map.entry("ㅁ", 6),  Map.entry("ㅂ", 7), // ㄹ=5, ㅁ=6, ㅂ=7으로 매핑
            Map.entry("ㅅ", 9),  Map.entry("ㅇ", 11), Map.entry("ㅈ", 12), // ㅅ=9, ㅇ=11, ㅈ=12로 매핑
            Map.entry("ㅊ", 14), Map.entry("ㅋ", 15), Map.entry("ㅌ", 16), // ㅊ=14, ㅋ=15, ㅌ=16으로 매핑
            Map.entry("ㅍ", 17), Map.entry("ㅎ", 18) // ㅍ=17, ㅎ=18로 매핑
    );

    /** [startInclusive, nextStartExclusive] 반환 (예: "가", "나") */
    public static String[] range(String initial) {
        Integer l = LINDEX.get(initial);
        if (l == null) return null;
        int start = SBASE + l * NCOUNT;
        int nextL = switch (l) {
            case 0 -> 2; case 2 -> 3; case 3 -> 5; case 5 -> 6; case 6 -> 7;
            case 7 -> 9; case 9 -> 11; case 11 -> 12; case 12 -> 14; case 14 -> 15;
            case 15 -> 16; case 16 -> 17; case 17 -> 18; default -> -1; // ㅎ
        };
        int nextStart = (nextL >= 0) ? (SBASE + nextL * NCOUNT) : (SBASE + SCOUNT);
        return new String[] { new String(Character.toChars(start)), new String(Character.toChars(nextStart)) };
    }
}
