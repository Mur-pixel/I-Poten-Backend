package com.cygnus.ipoten.quiz_question.controller.response_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.quiz_question.service.response.CreateQuizQuestionResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateQuizQuestionResponseForm {
    private final String message;
    private final Long questionId;
    private final QuestionType questionType;
    private final String questionText;
    private final String answerText;   // INITIALS/주관식

    public static CreateQuizQuestionResponseForm from(CreateQuizQuestionResponse response) {
        return new CreateQuizQuestionResponseForm(
                response.getMessage(),
                response.getQuestionId(),
                response.getQuestionType(),
                response.getQuestionText(),
                response.getAnswerText()
        );
    }
}
