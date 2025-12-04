package com.cygnus.ipoten.wordbook.entity.enums;

import java.util.Locale;

public enum FolderTermSort {
    CREATED_AT_DESC,
    TITLE_ASC,
    TITLE_DESC,
    STATUS_ASC,   // LEARNING -> DONE
    STATUS_DESC;  // DONE -> LEARNING

    public static FolderTermSort fromParam(String sortParam) {
        if (sortParam == null) return CREATED_AT_DESC;
        String s = sortParam.trim().toLowerCase(Locale.ROOT);

        if (s.startsWith("title")) {
            return s.contains("desc") ? TITLE_DESC : TITLE_ASC;
        }
        if (s.startsWith("status")) {
            return s.contains("desc") ? STATUS_DESC : STATUS_ASC;
        }
        return CREATED_AT_DESC;
    }
}
