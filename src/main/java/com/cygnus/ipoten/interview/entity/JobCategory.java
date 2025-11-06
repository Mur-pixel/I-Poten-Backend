package com.cygnus.ipoten.interview.entity;

public enum JobCategory {
    BACKEND(1),
    FRONTEND(2),
    EMBEDDED(3),
    AI(4),
    DEVOPS(5),
    WEBAPP(6);

    private final int id;

    JobCategory(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static JobCategory fromString(String jobName) {
        if (jobName == null) return BACKEND; // 기본값

        try {
            return valueOf(jobName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BACKEND; // 기본값
        }
    }
}
