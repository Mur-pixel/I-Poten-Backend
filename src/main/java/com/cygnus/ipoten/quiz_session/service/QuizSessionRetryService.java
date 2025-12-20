package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;

import java.util.List;

public interface QuizSessionRetryService {
    StartQuizSessionResponse startRetryWrongOnly(Long parentSessionId, Long accountId);
    StartQuizSessionResponse startRetryAll(Long sessionId, Long accountId);
}