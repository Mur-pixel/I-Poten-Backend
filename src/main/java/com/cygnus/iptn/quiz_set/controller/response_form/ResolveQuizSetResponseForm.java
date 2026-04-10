package com.cygnus.iptn.quiz_set.controller.response_form;

import com.cygnus.iptn.quiz_set.entity.QuizSet;
import com.cygnus.iptn.quiz_set.service.response.ResolveQuizSetResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ResolveQuizSetResponseForm {
    private final Long quizSetId;
    private final String title;
    private final int totalQuestions;

    public static ResolveQuizSetResponseForm from(ResolveQuizSetResult result) {
        return new ResolveQuizSetResponseForm(result.quizSetId(), result.title(),result.totalQuestions());
    }
}
