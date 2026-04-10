package com.cygnus.iptn.survey.entity;

/**
 * 설문 문항 유형을 나타내는 enum
 */
public enum SurveyQuestionType {
    SINGLE_CHOICE, // 객관식 단일 선택
    LINEAR_SCALE,  // 점수형 척도 선택
    LONG_TEXT      // 장문 서술형 입력
}