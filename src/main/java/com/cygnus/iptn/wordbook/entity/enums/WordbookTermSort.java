package com.cygnus.iptn.wordbook.entity.enums;

import java.util.Locale;

public enum WordbookTermSort {
    CREATED_AT_DESC,
    TITLE_ASC,
    TITLE_DESC,
    STATUS_ASC,
    STATUS_DESC;

    public static WordbookTermSort fromParam(String raw) {
        if (raw == null || raw.isBlank()) return CREATED_AT_DESC;

        String v = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        return switch (v) {
            case "title,asc" -> TITLE_ASC;
            case "title,desc" -> TITLE_DESC;
            case "status,asc" -> STATUS_ASC;
            case "status,desc" -> STATUS_DESC;
            case "createdat,desc", "created_at,desc" -> CREATED_AT_DESC;
            default -> CREATED_AT_DESC;
        };
    }
}