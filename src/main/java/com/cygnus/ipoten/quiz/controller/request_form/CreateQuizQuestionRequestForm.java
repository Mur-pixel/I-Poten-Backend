package com.cygnus.ipoten.quiz.controller.request_form;


import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz.service.request.CreateQuizQuestionRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor

public class CreateQuizQuestionRequestForm {

    private final Long categoryId;
    private final QuestionType questionType;
    private final String questionText;
    private final Integer questionAnswer;

    public CreateQuizQuestionRequest toCreateQuizQuestionRequest(Long termId) {
        return new CreateQuizQuestionRequest(termId, categoryId, questionType, questionText, questionAnswer);
    }

}
