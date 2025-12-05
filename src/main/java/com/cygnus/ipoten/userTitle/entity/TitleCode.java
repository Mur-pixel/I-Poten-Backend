package com.cygnus.ipoten.userTitle.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TitleCode {
    // 📌 기존
    EARLY_BIRD("EARLY_BIRD", "얼리버드", "아침 일찍 출석한 사람에게 주어지는 칭호"),
    BEGINNER("BEGINNER", "초심자", "첫 걸음을 내디딘 유저"),
    EXPERT("EXPERT", "전문가", "꾸준한 학습으로 전문성을 인정받은 유저"),
    LEGEND("LEGEND", "레전드", "최고의 성취를 달성한 전설적인 유저"),

    // 🎓 학습/지식 기반
    ROOKIE("ROOKIE", "루키", "학습을 시작한 신입"),
    LEARNER("LEARNER", "학습자", "꾸준히 배우고 있는 유저"),
    SCHOLAR("SCHOLAR", "학자", "깊은 지식을 쌓아가는 유저"),
    SPECIALIST("SPECIALIST", "전문가", "특정 분야의 전문가"),
    ARCHITECT("ARCHITECT", "아키텍트", "시스템을 설계하고 구조화하는 능력자"),
    GURU("GURU", "구루", "다른 사람을 가르칠 수 있는 지식의 달인"),

    // 🏅 시험/랭크 느낌
    CANDIDATE("CANDIDATE", "지원자", "도전을 시작한 사람"),
    CHALLENGER("CHALLENGER", "도전자", "목표를 향해 끊임없이 도전하는 유저"),
    ACHIEVER("ACHIEVER", "성취자", "여러 목표를 달성한 유저"),
    ELITE("ELITE", "엘리트", "상위권 성과를 낸 엘리트"),
    MASTER("MASTER", "마스터", "숙련도 높은 마스터"),
    GRANDMASTER("GRANDMASTER", "그랜드마스터", "최상위 등급에 도달한 유저"),

    // 🌟 학습 태도/습관 기반
    NIGHT_OWL("NIGHT_OWL", "올빼미", "늦은 밤에도 학습하는 유저"),
    HARD_WORKER("HARD_WORKER", "노력가", "끊임없이 노력하는 유저"),
    MARATHONER("MARATHONER", "마라토너", "긴 여정을 꾸준히 이어가는 유저"),
    SPRINTER("SPRINTER", "스프린터", "짧은 기간에 집중적으로 성과를 내는 유저");

    private final String code;
    private final String displayName;
    private final String description; // ✨ 추가된 부분
}