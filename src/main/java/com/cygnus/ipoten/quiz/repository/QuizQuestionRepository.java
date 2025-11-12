package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.QuizQuestion;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuizSet_IdOrderByOrderIndexAscIdAsc(Long setId);
    List<QuizQuestion> findByQuizSetIdAndQuestionTypeInOrderByOrderIndexAscIdAsc(Long setId, Collection<QuestionType> types);
}
