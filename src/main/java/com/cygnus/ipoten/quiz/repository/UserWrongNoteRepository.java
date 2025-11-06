package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.UserWrongNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserWrongNoteRepository extends JpaRepository<UserWrongNote, Long> {
    boolean existsByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
    Optional<UserWrongNote> findByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
}
