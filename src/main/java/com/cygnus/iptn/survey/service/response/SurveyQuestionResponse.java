package com.cygnus.iptn.survey.service.response;

import com.cygnus.iptn.survey.entity.SurveyQuestionType;

import java.util.List;

/**
 * 설문 문항 1개를 나타내는 서비스 응답 객체
 */
public record SurveyQuestionResponse(
        String code,                        // 문항 식별 코드 예 Q01
        String title,                       // 화면에 보여줄 문항 제목
        SurveyQuestionType type,            // 문항 타입 예 객관식 척도형 주관식
        boolean required,                   // 필수 응답 여부
        List<SurveyOptionResponse> options, // 객관식 문항일 때 선택지 목록
        SurveyScaleResponse scale           // 척도형 문항일 때 최소값 최대값 라벨 정보
) {
}