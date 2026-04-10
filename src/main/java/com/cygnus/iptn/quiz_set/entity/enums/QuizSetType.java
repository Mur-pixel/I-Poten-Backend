package com.cygnus.iptn.quiz_set.entity.enums;

public enum QuizSetType {
    CHOICE, OX, INITIALS, MIX;

    public static QuizSetType fromParam(String raw){
        if (raw == null || raw.isBlank()) return CHOICE;
        String s = raw.trim().toUpperCase(java.util.Locale.ROOT);

        // 1) DAILY_ 접두 허용
        if (s.startsWith("DAILY_")) s = s.substring("DAILY_".length());

        // 2) 동의어 정규화
        s = switch (s) {
            case "TRUE_FALSE", "TRUEFALSE", "TF", "T/F" -> "OX";
            case "INITIAL" -> "INITIALS";
            case "MIXED", "ALL", "COMBINED" -> "MIX";
            default -> s;
        };

        return switch (s) {
            case "CHOICE" -> CHOICE;
            case "OX" -> OX;
            case "INITIALS" -> INITIALS;
            case "MIX" -> MIX;
            default -> throw new IllegalArgumentException("Unknown part: " + raw);
        };
    }
}