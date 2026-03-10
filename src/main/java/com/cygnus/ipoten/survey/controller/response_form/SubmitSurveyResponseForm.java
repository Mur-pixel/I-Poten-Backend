package com.cygnus.ipoten.survey.controller.response_form;

import com.cygnus.ipoten.survey.service.response.SubmitSurveyResponse;

import java.time.Instant;

/**
 * 설문 제출 완료 응답 폼
 */
public record SubmitSurveyResponseForm(
        Long responseId,
        String formCode,
        Instant submittedAt
) {
    public static SubmitSurveyResponseForm from(SubmitSurveyResponse response) {
        return new SubmitSurveyResponseForm(
                response.responseId(),
                response.formCode(),
                response.submittedAt()
        );
    }
}
