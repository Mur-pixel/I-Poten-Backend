package com.cygnus.iptn.quiz_session.repository;

import com.cygnus.iptn.quiz_session.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizSessionTimelineRepository extends JpaRepository<QuizSession, Long>, QuizSessionTimelineRepositoryCustom {
}
