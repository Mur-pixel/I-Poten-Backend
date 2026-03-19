package com.cygnus.ipoten.survey.service;

import com.cygnus.ipoten.survey.service.response.GetActiveSurveyResponse;

public interface SurveyQueryService {
    GetActiveSurveyResponse getActiveSurvey(Long accountId);
}
