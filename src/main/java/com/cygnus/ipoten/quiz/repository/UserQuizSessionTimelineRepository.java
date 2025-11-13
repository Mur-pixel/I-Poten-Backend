package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.UserQuizSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuizSessionTimelineRepository extends JpaRepository<UserQuizSession, Long>, UserQuizSessionTimelineRepositoryCustom {
}
