package com.cygnus.ipoten.survey.repository;

import com.cygnus.ipoten.survey.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {
    List<SurveyQuestion> findBySurveyFormIdOrderByDisplayOrderAsc(Long surveyFormId);
}
