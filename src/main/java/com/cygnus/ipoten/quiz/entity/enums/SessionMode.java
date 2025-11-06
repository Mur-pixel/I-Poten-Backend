package com.cygnus.ipoten.quiz.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SessionMode {
    /**
     * FULL : 선택된 QuizSet 전체 문제를 포함하는 세션
     * - 사용자가 처음 풀거나, 전체 복습용으로 푸는 경우
     */
    FULL,

    /**
     * WRONG_ONLY : 이전 세션에서 틀린 문제만 포함하는 세션
     * - '오답노트 기반 다시 풀기' 기능
     * - UserQuizSession / WrongNote 기록을 참고해 세트 구성
     */
    WRONG_ONLY;

    @JsonCreator
    public static SessionMode from(String value) {
        return SessionMode.valueOf(value.toUpperCase());
    }
}