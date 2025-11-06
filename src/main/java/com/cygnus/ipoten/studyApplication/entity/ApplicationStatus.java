package com.cygnus.ipoten.studyApplication.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApplicationStatus {
    PENDING("대기중"),
    APPROVED("승인됨"),
    REJECTED("거절됨"),
    CANCELED("지원취소"),
    NOT_APPLIED("미신청");

    private final String description;
}
