package com.cygnus.iptn.survey.service.response;

/**
 * 척도형 문항의 범위와 양끝 라벨 정보를 나타내는 응답 객체
 */
public record SurveyScaleResponse(
        int min,          // 최소 점수
        int max,          // 최대 점수
        String minLabel,  // 최소 점수에 대한 설명 문구
        String maxLabel   // 최대 점수에 대한 설명 문구
) {
}