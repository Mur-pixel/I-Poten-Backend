package com.cygnus.iptn.quiz_session.service;

import com.cygnus.iptn.quiz_session.entity.QuizSession;

public interface QuizSessionRenameService {
    QuizSession renameTitle(Long sessionId, Long accountId, String rawTitle);
}
