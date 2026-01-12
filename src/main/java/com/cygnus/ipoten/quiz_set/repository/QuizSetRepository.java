package com.cygnus.ipoten.quiz_set.repository;

import com.cygnus.ipoten.quiz_set.entity.QuizSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSetRepository extends JpaRepository<QuizSet, Long> {

    Optional<QuizSet> findFirstByTitle(String title);

}
