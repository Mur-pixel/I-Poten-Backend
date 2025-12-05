package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
}
