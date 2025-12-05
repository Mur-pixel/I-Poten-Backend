package com.cygnus.ipoten.quiz_analytics.repository;

import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizSessionTimelineRepository extends JpaRepository<QuizSession, Long>, QuizSessionTimelineRepositoryCustom {
}
