package com.cygnus.iptn.survey.repository;

import com.cygnus.iptn.survey.entity.SurveyForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyFormRepository extends JpaRepository<SurveyForm, Integer> {

    Optional<SurveyForm> findFirstByActiveTrueOrderByVersionDesc();
}
