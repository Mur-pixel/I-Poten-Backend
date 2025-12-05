package com.cygnus.ipoten.quiz_question.service;

import com.cygnus.ipoten.quiz_question.service.request.CreateQuizQuestionRequest;
import com.cygnus.ipoten.quiz_question.service.response.CreateQuizQuestionResponse;

public interface QuizQuestionService {
    // 용어 기반 문제 등록
    CreateQuizQuestionResponse registerQuizQuestion(CreateQuizQuestionRequest request);

}
