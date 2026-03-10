package com.cygnus.ipoten.survey.service.request;

import java.util.List;

/**
 * 설문 제출 서비스 요청 객체
 */
public record SubmitSurveyResponseRequest(
        List<SubmitSurveyAnswerRequest> answers
) {
}
