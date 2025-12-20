package com.cygnus.ipoten.quiz_set.service;

import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByJobRoleRequest;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByWordbookRequest;
import com.cygnus.ipoten.quiz_session.service.request.CreateQuizSessionRequest;
import com.cygnus.ipoten.quiz_set.service.request.CreateQuizSetByCategoryRequest;
import com.cygnus.ipoten.quiz.service.response.BuiltQuizSetResponse;
import com.cygnus.ipoten.quiz_session.service.response.CreateQuizSessionResponse;
import com.cygnus.ipoten.quiz.service.response.CreateQuizSetByCategoryResponse;

public interface QuizSetService {
    // 카테고리 기반 퀴즈 자동 생성
    CreateQuizSetByCategoryResponse registerQuizSetByCategory(CreateQuizSetByCategoryRequest request);

    // 세트 생성 + 문항 ID까지 만들어서 반환
    BuiltQuizSetResponse registerQuizSetByCategoryReturningQuestions(CreateQuizSetByCategoryRequest request);
    BuiltQuizSetResponse registerQuizSetByWordbookReturningQuestions(CreateQuizSetByWordbookRequest request);
    BuiltQuizSetResponse registerQuizSetByJobRole(CreateQuizSetByJobRoleRequest request);
}
