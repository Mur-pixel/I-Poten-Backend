package com.cygnus.iptn.quiz_set.service;

import com.cygnus.iptn.quiz_set.service.request.CreateQuizSetByWordbookRequest;
import com.cygnus.iptn.quiz_set.service.request.CreateQuizSetByCategoryRequest;
import com.cygnus.iptn.quiz_set.service.response.BuiltQuizSetResponse;
import com.cygnus.iptn.quiz_set.service.response.CreateQuizSetByCategoryResponse;

public interface QuizSetService {
    // 세트 생성 + 문항 ID까지 만들어서 반환
    BuiltQuizSetResponse registerQuizSetByCategoryReturningQuestions(CreateQuizSetByCategoryRequest request);
    BuiltQuizSetResponse registerQuizSetByWordbookReturningQuestions(CreateQuizSetByWordbookRequest request);
}
