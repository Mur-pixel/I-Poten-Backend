package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.controller.request_form.SubmitQuizSessionRequestForm;
import com.cygnus.ipoten.quiz.controller.response_form.SubmitQuizSessionResponseForm;
import com.cygnus.ipoten.quiz.entity.SessionAnswer;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz.service.response.StartUserQuizSessionResponse;

import java.util.List;

public interface UserQuizAnswerService {
    StartUserQuizSessionResponse startFromQuizSet(Long accountId, Long quizSetId, List<Long> questionIds, SeedMode seedMode, Long fixedSeed);
    StartUserQuizSessionResponse startRetryWrongOnly(Long parentSessionId, Long accountId);
    SubmitQuizSessionResponseForm submitSession(Long sessionId, Long accountId, SubmitQuizSessionRequestForm form);
    void saveWrongNotes(List<SessionAnswer> answers, Long accountId);
}