package com.cygnus.ipoten.studyroom.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StudyLocation {
    ONLINE("온라인"),
    SEOUL("서울"),
    GYEONGGI("경기"),
    INCHEON("인천"),
    GANGWON("강원"),
    CHUNGBUK("충북"),
    CHUNGNAM("충남"),
    DAEJEON("대전"),
    SEJONG("세종"),
    JEONBUK("전북"),
    JEONNAM("전남"),
    GWANGJU("광주"),
    GYEONGBUK("경북"),
    GYEONGNAM("경남"),
    DAEGU("대구"),
    ULSAN("울산"),
    BUSAN("부산"),
    JEJU("제주");

    private final String koreanName;
}