package com.cygnus.ipoten.quiz_session_scope.service;

import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session_scope.value_objects.JobScope;

public interface JobRoleScopeService {

    /** 직무 범위(JobScope)에 따라 QuizSet + QuestionIds를 생성 */
    BuiltQuizSetResponse buildQuizSet(JobScope jobScope);
}
