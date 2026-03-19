package com.cygnus.ipoten.survey.controller.request_form;

import com.cygnus.ipoten.survey.service.request.SubmitSurveyResponseRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 설문 제출 요청 폼
 */
public record SubmitSurveyResponseRequestForm(
        @NotEmpty
        List<@Valid SubmitSurveyAnswerRequestForm> answers
) {
    public SubmitSurveyResponseRequest toServiceRequest() {
        return new SubmitSurveyResponseRequest(
                answers.stream()
                        .map(SubmitSurveyAnswerRequestForm::toServiceRequest)
                        .toList()
        );
    }
}
