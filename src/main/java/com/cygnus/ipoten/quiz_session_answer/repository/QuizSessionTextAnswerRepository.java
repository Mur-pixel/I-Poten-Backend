package com.cygnus.ipoten.quiz_session_answer.repository;

import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionTextAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSessionTextAnswerRepository extends JpaRepository<QuizSessionTextAnswer, Long> {
    List<QuizSessionTextAnswer> findByQuizSession_Id(Long sessionId);
}
