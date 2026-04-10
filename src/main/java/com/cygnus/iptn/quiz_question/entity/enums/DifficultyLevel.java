package com.cygnus.iptn.quiz_question.entity.enums;

import java.util.Locale;

public enum DifficultyLevel {
    EASY, MEDIUM, HARD, MIX;

    public static DifficultyLevel fromParam(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if ("MIX".equals(s)) return null; // MIX면 서비스에서 난이도 조건을 걸지 않음(null 취급)
        return DifficultyLevel.valueOf(s);
    }
}
