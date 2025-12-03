package com.cygnus.ipoten.quiz_wrongnote.service;

import com.cygnus.ipoten.quiz_session.entity.SessionAnswer;
import com.cygnus.ipoten.quiz_session.service.response.StartUserQuizSessionResponse;

import java.util.List;

public interface WrongNoteService {
    StartUserQuizSessionResponse startRetryWrongOnly(Long parentSessionId, Long accountId);
    void saveWrongNotes(List<SessionAnswer> answers, Long accountId);
}