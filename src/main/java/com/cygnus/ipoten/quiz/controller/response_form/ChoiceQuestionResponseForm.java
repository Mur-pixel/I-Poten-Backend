package com.cygnus.ipoten.quiz.controller.response_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.cygnus.ipoten.quiz_question.service.response.ChoiceQuestionRead;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChoiceQuestionResponseForm(
        Long id,
        String questionText,
        List<String> choices,
        Integer correctIndex,
        String explanation
) {
    public static ChoiceQuestionResponseForm from(ChoiceQuestionRead read) {
        return new ChoiceQuestionResponseForm(
                read.id(), read.questionText(), read.choices(), read.correctIndex(), read.explanation()
        );
    }

}
