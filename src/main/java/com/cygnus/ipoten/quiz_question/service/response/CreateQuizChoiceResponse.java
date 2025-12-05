package com.cygnus.ipoten.quiz_question.service.response;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateQuizChoiceResponse {

    private final Long quizChoiceId;
    private final String choiceText;
    private final boolean isAnswer;

    public static CreateQuizChoiceResponse from(QuizChoice quizChoice) {
        return new CreateQuizChoiceResponse(
                quizChoice.getId(),
                quizChoice.getChoiceText(),
                quizChoice.isAnswer()
        );
    }
}
