package com.cygnus.ipoten.quiz_session_generator.service.generator;

import lombok.Getter;
import lombok.Setter;

/** 난이도별 동작 파라미터 묶음 */
@Getter @Setter
public class DifficultyProfile {
    /** 오답 후보 개수(정답 제외 최종 보기 수는 1 + maxDistractors 중 3개까지) */
    private int maxDistractors = 6;

    /** 유사도 하한/상한 (0~1, 하한 이상만 오답 후보로 채택) */
    private double similarityMin = 0.15;
    private double similarityMax = 0.95;

    /** 지문 변형 확률 (부정/치환을 시도할 확률) */
    private double negateProb = 0.0;
    private double replaceProb = 0.0;

    /** 보기 길이 편향 허용치(문자 수 차이 비율 한도) */
    private double lengthBiasTolerance = 0.35;

    /** INITIALS 힌트: 글자수 공개 여부/공개 글자수 */
    private boolean revealLength = true;
    private int revealInitialsCount = 0; // 0이면 공개 안 함

    /** 보기 개수(최종 랜더링) — 보통 4지선다 */
    private int optionCount = 4;
}
