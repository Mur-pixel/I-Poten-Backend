package com.cygnus.iptn.survey.service.response;

/**
 * 객관식 문항의 선택지 1개를 나타내는 응답 객체
 */
public record SurveyOptionResponse(
        String code,   // 내부 저장/비교용 선택지 코드
        String label   // 화면에 보여줄 선택지 문구
) {
}
