package com.cygnus.ipoten.survey.service.request;

/**
 * 설문 문항 1개에 대한 서비스 요청 객체
 */
public record SubmitSurveyAnswerRequest(
        String questionCode,
        String selectedOptionCode,
        Integer scaleValue,
        String textAnswer
) {
}
