package com.cygnus.ipoten.quiz_daily.repository;

import com.cygnus.ipoten.quiz_daily.entity.DailyQuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyQuizAnswerRepository extends JpaRepository<DailyQuizAnswer, Long> {
    Optional<DailyQuizAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
}