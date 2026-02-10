package com.cygnus.ipoten.quiz_question.repository;

import com.cygnus.ipoten.quiz_question.entity.QuizTextAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuizTextAnswerRepository extends JpaRepository<QuizTextAnswer, Long> {
    List<QuizTextAnswer> findByQuizQuestion_IdIn(List<Long> quizQuestionIds);

    @Transactional
    void deleteByQuizQuestion_Id(Long quizQuestionId);
}
