package com.cygnus.ipoten.quiz.controller.response_form;

import com.cygnus.ipoten.quiz.entity.QuizChoice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateQuizChoiceListResponseForm {

    private final String message;
    private final List<CreateQuizChoiceResponseForm> choices;

    public static CreateQuizChoiceListResponseForm from(List<QuizChoice> quizChoices) {
        List<CreateQuizChoiceResponseForm> responses = quizChoices.stream()
                .map(CreateQuizChoiceResponseForm::from)
                .toList();

        return new CreateQuizChoiceListResponseForm(
                "문제의 보기와 답이 정상적으로 등록되었습니다.",
                responses
        );
    }
}