package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;

public interface QuizSessionRetryService {
    StartQuizSessionResponse startRetryWrongOnly(Long parentSessionId, Long accountId);
    StartQuizSessionResponse startRetryAll(Long sessionId, Long accountId);
    StartQuizSessionResponse startQuickRetry(Long accountId);
    StartQuizSessionResponse startQuickRetry(Long accountId, int days);
}