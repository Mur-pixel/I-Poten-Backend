package com.cygnus.iptn.quiz_question.util;

import java.text.Normalizer;
import java.util.Locale;

public final class TextAnswerNormalizer {

    private TextAnswerNormalizer() {}

    public static String normalize(String text) {
        if (text == null) return "";

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        normalized = normalized.replaceAll("\\s+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        return normalized;
    }

    public static boolean equalsIgnoringWhitespace(String left, String right) {
        String normalizedLeft = normalize(left);
        return !normalizedLeft.isBlank() && normalizedLeft.equals(normalize(right));
    }
}
