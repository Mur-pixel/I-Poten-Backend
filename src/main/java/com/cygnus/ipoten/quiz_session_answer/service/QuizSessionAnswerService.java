package com.cygnus.ipoten.quiz_session_answer.service;

import com.cygnus.ipoten.quiz_session_answer.controller.request_form.SubmitQuizSessionRequestForm;
import com.cygnus.ipoten.quiz_session.controller.response_form.SubmitQuizSessionResponseForm;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.quiz_session_scope.value_objects.SessionSource;

import java.util.List;

public interface QuizSessionAnswerService {
    StartQuizSessionResponse startFromQuizSet(Long accountId, Long quizSetId, List<Long> questionIds, SeedMode seedMode, Long fixedSeed, String customTitle);
    SubmitQuizSessionResponseForm submitSession(Long sessionId, Long accountId, SubmitQuizSessionRequestForm form);
    StartQuizSessionResponse startFromScope(Long accountId, SessionSource source, List<Long> pickedQuestionIds, SeedMode mode,long seedValue, String customTitle);
    StartQuizSessionResponse loadForPlay(Long accountId, Long sessionId);
}