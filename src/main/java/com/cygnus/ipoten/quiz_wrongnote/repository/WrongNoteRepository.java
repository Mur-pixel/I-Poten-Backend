package com.cygnus.ipoten.quiz_wrongnote.repository;

import com.cygnus.ipoten.quiz_wrongnote.entity.WrongNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WrongNoteRepository extends JpaRepository<WrongNote, Long> {
    boolean existsByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
    Optional<WrongNote> findByAccount_IdAndQuizQuestion_Id(Long accountId, Long quizQuestionId);
}
