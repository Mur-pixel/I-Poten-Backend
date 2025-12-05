package com.cygnus.ipoten.quiz_question.repository;

import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuizSet_IdOrderByIdAsc(Long setId);
    List<QuizQuestion> findByQuizSetIdAndQuestionTypeInOrderByIdAsc(Long setId, Collection<QuestionType> types);
}
