package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;

import java.util.List;

public interface QuizWrongNoteService {
    void saveWrongNotes(List<QuizSessionAnswer> answers, Long accountId);
}
