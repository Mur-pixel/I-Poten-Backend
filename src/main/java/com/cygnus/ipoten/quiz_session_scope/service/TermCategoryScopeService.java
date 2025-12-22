package com.cygnus.ipoten.quiz_session_scope.service;

import com.cygnus.ipoten.quiz_set.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session_scope.value_objects.TermCategoryScope;

public interface TermCategoryScopeService {

    /** 카테고리 범위(CategoryScope)에 따라 QuizSet + QuestionIds를 생성 */
    BuiltQuizSetResponse buildQuizSet(TermCategoryScope termCategoryScope);
}
