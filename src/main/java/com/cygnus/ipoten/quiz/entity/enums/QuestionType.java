package com.cygnus.ipoten.quiz.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QuestionType {
    CHOICE,     // 4지선다 객관식 문제
    OX,         // OX 진위형 문제
    INITIALS;   // 설명 -> 용어의 '초성'을 맞히는 문제

    @JsonCreator
    public static QuestionType from(String value){
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 문제 유형입니다: null 또는 빈 값. [CHOICE, OX, INITIALS] 중 하나를 사용하세요.");
        }
        try {
            return QuestionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 문제 유형입니다: " + value + ". [CHOICE, OX, INITIALS] 중 하나를 사용하세요.");
        }
    }
}
