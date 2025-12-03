package com.cygnus.ipoten.quiz.controller.response_form;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateQuizChoiceResponseForm {
    private final Long quizChoiceId;
    private final String choiceText;
    private final boolean isAnswer;

    public static CreateQuizChoiceResponseForm from(QuizChoice quizChoice) {
        return new CreateQuizChoiceResponseForm(
                quizChoice.getId(),
                quizChoice.getChoiceText(),
                quizChoice.isAnswer()
        );
    }
}
