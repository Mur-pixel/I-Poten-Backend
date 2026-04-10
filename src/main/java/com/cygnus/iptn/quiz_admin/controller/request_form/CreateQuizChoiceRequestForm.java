package com.cygnus.iptn.quiz_admin.controller.request_form;

import com.cygnus.iptn.quiz_question.entity.enums.QuestionType;
import com.cygnus.iptn.quiz_question.service.request.CreateQuizChoiceRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateQuizChoiceRequestForm {

    private QuestionType questionType;
    private List<ChoiceForm> choices;

    @Getter
    @RequiredArgsConstructor
    public static class ChoiceForm {
        private final String choiceText;
        private final boolean isAnswer;
        private final String explanation;
    }

    public List<CreateQuizChoiceRequest> toCreateQuizChoiceRequest(Long quizQuestionId) {
        return choices.stream()
                .map(choice -> new CreateQuizChoiceRequest(
                        quizQuestionId,
                        choice.getChoiceText(),
                        choice.isAnswer()
                )).toList();
    }

}
