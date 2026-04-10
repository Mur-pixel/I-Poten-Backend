package com.cygnus.iptn.survey.service;

import com.cygnus.iptn.survey.service.request.SubmitSurveyResponseRequest;
import com.cygnus.iptn.survey.service.response.SubmitSurveyResponse;

public interface SurveySubmitService {
    SubmitSurveyResponse submitActiveSurvey(Long accountId, SubmitSurveyResponseRequest request);
}
