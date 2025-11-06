package com.cygnus.ipoten.quiz.service;

import com.cygnus.ipoten.quiz.entity.QuizChoice;
import com.cygnus.ipoten.quiz.service.request.CreateQuizChoiceRequest;

import java.util.List;

public interface QuizChoiceService {
    // 용어 기반 문제 등록
    List<QuizChoice> registerQuizChoices(Long quizQuestionId, List<CreateQuizChoiceRequest> requestList);
}
