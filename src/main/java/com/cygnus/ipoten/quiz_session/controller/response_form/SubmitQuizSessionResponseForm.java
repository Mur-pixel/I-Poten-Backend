package com.cygnus.ipoten.quiz_session.controller.response_form;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitQuizSessionResponseForm {

    private final Long sessionId;
    private final int total;
    private final int correct;
    private final Long elapsedMs;
    private final List<Item> details;

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {

        private final Long quizQuestionId;
        private final Long selectedChoiceId;
        private final String submittedText;
        private final boolean correct;
    }
}
