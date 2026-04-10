package com.cygnus.iptn.survey.repository;

import com.cygnus.iptn.survey.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {
    List<SurveyQuestion> findBySurveyFormIdOrderByDisplayOrderAsc(Long surveyFormId);
}
