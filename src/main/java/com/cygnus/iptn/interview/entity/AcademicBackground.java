package com.cygnus.iptn.interview.entity;

public enum AcademicBackground {
    NON_MAJOR(1, "비전공자"),
    MAJOR(2, "전공자");

    private final int id;
    private final String description;

    AcademicBackground(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static AcademicBackground fromString(String description) {

        for (AcademicBackground background : values()) {
            if (background.getDescription().equals(description)) {
                return background;
            }
        }
        return MAJOR; // 기본값
    }
}
