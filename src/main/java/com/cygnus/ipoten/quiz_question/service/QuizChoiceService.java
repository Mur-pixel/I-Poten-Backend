package com.cygnus.ipoten.quiz_question.service;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.service.request.CreateQuizChoiceRequest;

import java.util.List;

public interface QuizChoiceService {
    // 용어 기반 문제 등록
    List<QuizChoice> registerQuizChoices(Long quizQuestionId, List<CreateQuizChoiceRequest> requestList);
}
