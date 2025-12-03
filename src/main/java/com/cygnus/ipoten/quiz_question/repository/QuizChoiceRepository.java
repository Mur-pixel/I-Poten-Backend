package com.cygnus.ipoten.quiz_question.repository;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuizChoiceRepository extends JpaRepository<QuizChoice, Long> {
    List<QuizChoice> findByQuizQuestionIdIn(List<Long> questionIds);
    List<QuizChoice> findByQuizQuestionIdInOrderByIdAsc(Collection<Long> questionIds);
}