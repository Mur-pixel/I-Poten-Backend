package com.cygnus.iptn.survey.repository;

import com.cygnus.iptn.survey.entity.SurveyQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyQuestionOptionRepository extends JpaRepository<SurveyQuestionOption, Long> {
    List<SurveyQuestionOption> findBySurveyQuestionIdInOrderBySurveyQuestionIdAscDisplayOrderAsc(List<Long> surveyQuestionIds);
}
