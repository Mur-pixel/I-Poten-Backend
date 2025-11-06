package com.cygnus.ipoten.interview.entity;

public enum ExperienceLevel {
    NEWBIE(1, "신입"),
    THREE_YEARS_OR_LESS(2, "3년 이하"),
    FIVE_YEARS_OR_LESS(3, "5년 이하"),
    TEN_YEARS_OR_LESS(4, "10년 이하"),
    TEN_YEARS_OR_MORE(5, "10년 이상");

    private final int id;
    private final String description;

    ExperienceLevel(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static ExperienceLevel fromString(String description) {
        if (description == null) return NEWBIE; // 기본값

        for (ExperienceLevel level : values()) {
            if (level.getDescription().equals(description)) {
                return level;
            }
        }
        return NEWBIE; // 기본값
    }
}
