package com.cygnus.ipoten.quiz_label.repository;

import com.cygnus.ipoten.quiz_label.entity.QuizLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizLabelRepository extends JpaRepository<QuizLabel, Long> {
    Optional<QuizLabel> findByKey(String key);
}