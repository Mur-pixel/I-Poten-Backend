package com.cygnus.iptn.quiz_question.service.request;

import com.cygnus.iptn.quiz_question.entity.QuizChoice;
import com.cygnus.iptn.quiz_question.entity.QuizQuestion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateQuizChoiceRequest {
    private final Long quizQuestionId;
    private final String choiceText;
    private final boolean isAnswer;

    public QuizChoice toQuizChoice(QuizQuestion quizQuestion) {
        return new QuizChoice(quizQuestion, choiceText, isAnswer);
    }

}
