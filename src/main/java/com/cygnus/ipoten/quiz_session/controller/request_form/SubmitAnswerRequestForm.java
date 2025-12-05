package com.cygnus.ipoten.quiz_session.controller.request_form;

import com.cygnus.ipoten.quiz_session.service.request.SubmitAnswerRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SubmitAnswerRequestForm {

    private final Long accountId;
    private List<AnswerForm> answers;

    @Getter
    @RequiredArgsConstructor
    public static class AnswerForm {
        private final Long quizQuestionId;
        private final Long selectedChoiceId;
    }

    public List<SubmitAnswerRequest> toSubmitAnswerRequests() {
        return answers.stream()
                .map(answer -> new SubmitAnswerRequest(
                        accountId,
                        answer.getQuizQuestionId(),
                        answer.getSelectedChoiceId()
                )).toList();
    }
}
