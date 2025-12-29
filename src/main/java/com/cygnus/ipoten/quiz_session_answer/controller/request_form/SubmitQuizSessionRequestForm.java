package com.cygnus.ipoten.quiz_session_answer.controller.request_form;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    private List<AnswerForm> answers;

    private Long elapsedMs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerForm {

        @NotNull
        private Long quizQuestionId;

        // 프론트가 quizChoiceId로 보내도 받게(호환)
        @JsonAlias({"selectedChoiceId", "quizChoiceId", "quiz_choice_id", "choiceId"})
        private Long selectedChoiceId;

        // INITIALS 제출용
        @JsonAlias({"textAnswer", "answerText", "submittedText"})
        private String textAnswer;
    }
}