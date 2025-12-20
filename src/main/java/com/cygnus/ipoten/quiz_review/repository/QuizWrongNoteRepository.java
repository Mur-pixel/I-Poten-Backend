package com.cygnus.ipoten.quiz_review.repository;

import com.cygnus.ipoten.quiz_review.entity.QuizWrongNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizWrongNoteRepository extends JpaRepository<QuizWrongNote, Long> {
    boolean existsByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
    Optional<QuizWrongNote> findByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
}
