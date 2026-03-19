package com.cygnus.ipoten.survey.service;

import com.cygnus.ipoten.survey.service.request.SubmitSurveyResponseRequest;
import com.cygnus.ipoten.survey.service.response.SubmitSurveyResponse;

public interface SurveySubmitService {
    SubmitSurveyResponse submitActiveSurvey(Long accountId, SubmitSurveyResponseRequest request);
}
