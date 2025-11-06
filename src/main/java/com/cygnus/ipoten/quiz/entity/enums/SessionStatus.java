package com.cygnus.ipoten.quiz.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SessionStatus {
    /**
     * IN_PROGRESS : 사용자가 세션을 시작했지만 아직 다 풀지 않고 진행 중인 상태
     */
    IN_PROGRESS,

    /**
     * SUBMITTED : 사용자가 직접 "제출하기"를 눌러 끝내 세션으로 점수와 결과가 확정됨
     */
    SUBMITTED,

    /**
     * EXPIRED : 사용자가 끝까지 제출하지 않았는데, 제한 시간 또는 유효 기간이 지나 자동 종료된 세션
     */
    EXPIRED;

    @JsonCreator
    public static SessionStatus from(String value) {
        return SessionStatus.valueOf(value.toUpperCase());
    }
}

