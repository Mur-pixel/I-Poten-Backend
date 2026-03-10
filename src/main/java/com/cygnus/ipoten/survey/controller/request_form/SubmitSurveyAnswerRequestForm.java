package com.cygnus.ipoten.survey.controller.request_form;

import com.cygnus.ipoten.survey.service.request.SubmitSurveyAnswerRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 설문 문항 1개에 대한 제출 요청 폼
 */
public record SubmitSurveyAnswerRequestForm(
        @NotBlank
        String questionCode,        // 답변할 문항 코드

        String selectedOptionCode,  // 객관식 선택지 코드

        Integer scaleValue,         // 척도형 선택 값

        @Size(max = 2000)
        String textAnswer           // 주관식 답변
) {
    public SubmitSurveyAnswerRequest toServiceRequest() {
        return new SubmitSurveyAnswerRequest(
                questionCode,
                selectedOptionCode,
                scaleValue,
                textAnswer
        );
    }
}
