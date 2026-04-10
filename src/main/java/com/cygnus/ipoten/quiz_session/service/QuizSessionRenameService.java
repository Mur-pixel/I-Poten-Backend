package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_session.entity.QuizSession;

public interface QuizSessionRenameService {
    QuizSession renameTitle(Long sessionId, Long accountId, String rawTitle);
}
