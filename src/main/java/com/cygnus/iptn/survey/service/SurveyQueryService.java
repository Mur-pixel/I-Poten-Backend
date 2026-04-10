package com.cygnus.iptn.survey.service;

import com.cygnus.iptn.survey.service.response.GetActiveSurveyResponse;

public interface SurveyQueryService {
    GetActiveSurveyResponse getActiveSurvey(Long accountId);
}
