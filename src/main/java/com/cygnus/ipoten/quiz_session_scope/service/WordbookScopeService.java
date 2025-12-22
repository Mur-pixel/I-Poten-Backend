package com.cygnus.ipoten.quiz_session_scope.service;

import com.cygnus.ipoten.quiz_set.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session_scope.value_objects.WordbookScope;

public interface WordbookScopeService {

    /** 단어장 범위(WordbookScope)에 따라 QuizSet + QuestionIds를 생성 */
    BuiltQuizSetResponse buildQuizSet(WordbookScope wordbookScope);
}
