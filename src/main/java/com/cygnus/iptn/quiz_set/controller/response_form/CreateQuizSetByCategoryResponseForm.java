package com.cygnus.iptn.quiz_set.controller.response_form;

import com.cygnus.iptn.quiz_set.service.response.CreateQuizSetByCategoryResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateQuizSetByCategoryResponseForm {
    private final String message;
    private final Long quizSetId;
    private final String title;

    public static CreateQuizSetByCategoryResponseForm from(CreateQuizSetByCategoryResponse response) {
        return new CreateQuizSetByCategoryResponseForm(
                response.getMessage(),
                response.getQuizSetId(),
                response.getTitle()
        );
    }
}
