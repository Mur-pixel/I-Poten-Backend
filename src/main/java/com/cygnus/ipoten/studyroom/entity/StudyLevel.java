package com.cygnus.ipoten.studyroom.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyLevel {
    ALL("누구나"),
    NEWBIE("신입"),
    JUNIOR("주니어 개발자"),
    MID("중급 개발자"),
    SENIOR("시니어 개발자");

    private final String koreaLevel;
}
