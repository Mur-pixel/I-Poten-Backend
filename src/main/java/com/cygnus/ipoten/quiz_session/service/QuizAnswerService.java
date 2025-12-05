package com.cygnus.ipoten.quiz_session.service;

import com.cygnus.ipoten.quiz_session.controller.request_form.SubmitQuizSessionRequestForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SubmitQuizSessionResponseForm;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.service.response.StartUserQuizSessionResponse;

import java.util.List;

public interface QuizAnswerService {
    StartUserQuizSessionResponse startFromQuizSet(Long accountId, Long quizSetId, List<Long> questionIds, SeedMode seedMode, Long fixedSeed);
    SubmitQuizSessionResponseForm submitSession(Long sessionId, Long accountId, SubmitQuizSessionRequestForm form);
}