package com.cygnus.iptn.survey.service.response;

import java.util.List;

public record GetActiveSurveyResponse(
        String formCode,
        int version,
        String title,
        String description,
        List<SurveyQuestionResponse> questions
) {
}
