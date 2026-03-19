package com.cygnus.ipoten.survey.service.response;

import java.time.Instant;

/**
 * 설문 제출 완료 서비스 응답 객체
 */
public record SubmitSurveyResponse(
        Long responseId,
        String formCode,
        Instant submittedAt
) {
}
