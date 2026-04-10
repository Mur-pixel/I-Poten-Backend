package com.cygnus.ipoten.quiz_question.util;

public final class HangulInitials {
    private HangulInitials() {}

    private static final char HANGUL_BASE = 0xAC00;
    private static final char HANGUL_LAST = 0xD7A3;
    private static final int  CHO_COUNT = 19;
    private static final int  JUNG_COUNT = 21;
    private static final int  JONG_COUNT = 28;
    private static final int  SYLLABLE_BLOCK = JUNG_COUNT * JONG_COUNT; // 588

    private static final String[] CHO = {
            "ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"
    };

    /** 한글 음절은 초성만 추출, 공백은 유지, 비한글(영문/숫자/기호)은 그대로 */
    public static String toInitialsHint(String answerText) {
        if (answerText == null || answerText.isBlank()) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < answerText.length(); i++) {
            char ch = answerText.charAt(i);

            if (Character.isWhitespace(ch)) {
                sb.append(' ');
                continue;
            }

            if (ch >= HANGUL_BASE && ch <= HANGUL_LAST) {
                int syllableIndex = ch - HANGUL_BASE;
                int choIndex = syllableIndex / SYLLABLE_BLOCK;
                if (choIndex >= 0 && choIndex < CHO_COUNT) sb.append(CHO[choIndex]);
                else sb.append(ch);
            } else {
                sb.append(ch);
            }
        }

        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }
}