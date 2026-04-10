package com.cygnus.iptn.survey.repository;

import com.cygnus.iptn.survey.entity.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {
    boolean existsBySurveyFormIdAndAccountId(Long surveyFormId, Long accountId);
}
