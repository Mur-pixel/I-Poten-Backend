package com.cygnus.iptn.quiz_label.repository;

import com.cygnus.iptn.quiz_label.entity.QuizQuestionLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizQuestionLabelRepository extends JpaRepository<QuizQuestionLabel, Long> {
    boolean existsByQuizQuestion_IdAndQuizLabel_Id(Long quizQuestionId, Long quizLabelId);
}