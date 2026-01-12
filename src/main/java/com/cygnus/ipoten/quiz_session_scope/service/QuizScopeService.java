package com.cygnus.ipoten.quiz_session_scope.service;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_session.service.response.StartQuizSessionResponse;
import com.cygnus.ipoten.quiz_session_scope.value_objects.ScopeCondition;
import com.cygnus.ipoten.quiz_session_scope.value_objects.SeedPolicy;

import java.util.List;

public interface QuizScopeService {

    /** WORDBOOK / TERM_CATEGORY / JOB 범위 조건에 따라 세트 생성 + 세션 시작까지 한 번에 처리 */
    StartQuizSessionResponse startScopedSession(Long accountId, ScopeCondition condition, String customTitle);

    /** 이미 존재하는 세트(setId)에서 바로 세션 시작 */
    StartQuizSessionResponse startFromSet(Long accountId, Long quizSetId, Integer count, String typeRaw, DifficultyLevel level, SeedPolicy seedPolicy, String customTitle);

    StartQuizSessionResponse loadSessionForPlay(Long accountId, Long sessionId);
}
