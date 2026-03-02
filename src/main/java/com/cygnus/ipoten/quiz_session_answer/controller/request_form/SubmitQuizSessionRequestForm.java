package com.cygnus.ipoten.quiz_session_answer.controller.request_form;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitQuizSessionRequestForm {

    @NotEmpty
    @Valid
    private List<AnswerForm> answers;
    private Long elapsedMs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerForm {

        @NotNull
        private Long quizQuestionId;
        private Long selectedChoiceId;
        private String textAnswer;
    }
}