package com.cygnus.ipoten.quiz.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

public enum JobRole {
    GENERAL("General"),
    FRONTEND("Frontend"),
    BACKEND("Backend"),
    DATABASE("Database"),
    NETWORK("Network"),
    OPERATING_SYSTEM("Operating System"),
    DATA_STRUCTURES_ALGORITHMS("Data Structure & Algorithm"),
    SECURITY("Security"),
    SOFTWARE_ENGINEERING("Software Engineering"),
    DEVOPS_CLOUD("DevOps / Cloud"),
    COMPUTER_SCIENCE("Computer Science"),
    AI_DATA_MACHINE_LEARNING("AI / Data / Machine Learning"),
    EMBEDDED_IOT_SYSTEM_PROGRAMMING("Embedded / IoT / System Programming");

    private final String label;

    JobRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 직렬화 기본값은 상수명 유지(프론트/DB와의 일관성) */
    @JsonValue
    public String toJson() {
        return name();
    }

    // -------- 유연 파싱 지원 --------
    private static final Map<String, JobRole> LOOKUP = new HashMap<>();
    static {
        for (JobRole r : values()) {
            LOOKUP.put(norm(r.name()), r);
            LOOKUP.put(norm(r.label), r);
        }
        // 흔한 별칭/약어
        LOOKUP.put("fe", FRONTEND);
        LOOKUP.put("be", BACKEND);
        LOOKUP.put("db", DATABASE);
        LOOKUP.put("os", OPERATING_SYSTEM);
        LOOKUP.put("dsa", DATA_STRUCTURES_ALGORITHMS);
        LOOKUP.put("devops", DEVOPS_CLOUD);
        LOOKUP.put("cloud", DEVOPS_CLOUD);
        LOOKUP.put("cs", COMPUTER_SCIENCE);
        LOOKUP.put("ai", AI_DATA_MACHINE_LEARNING);
        LOOKUP.put("ml", AI_DATA_MACHINE_LEARNING);
        LOOKUP.put("iot", EMBEDDED_IOT_SYSTEM_PROGRAMMING);
        LOOKUP.put("embedded", EMBEDDED_IOT_SYSTEM_PROGRAMMING);
        LOOKUP.put("systemprogramming", EMBEDDED_IOT_SYSTEM_PROGRAMMING);
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[\\s/_&\\-\\.]+", "");
    }

    @JsonCreator
    public static JobRole from(String raw) {
        if (raw == null || raw.isBlank()) return GENERAL;
        JobRole byAlias = LOOKUP.get(norm(raw));
        if (byAlias != null) return byAlias;
        try {
            // "Operating System" -> OPERATING_SYSTEM 식의 단순 매핑도 시도
            return JobRole.valueOf(raw.trim().toUpperCase().replace(' ', '_'));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown JobRole: " + raw);
        }
    }
}
