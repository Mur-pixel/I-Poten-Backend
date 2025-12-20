package com.cygnus.ipoten.quiz_session.service.response;

import com.cygnus.ipoten.quiz_session_answer.entity.QuizSessionAnswer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class SubmitAnswerResponse {
    private final Long questionId;
    private final Long selectedChoiceId;
    private final Long userAnswerId;
    private final boolean isCorrect;
    private final Instant submittedAt;

    public static SubmitAnswerResponse from(QuizSessionAnswer answer) {
        return new SubmitAnswerResponse(
                answer.getQuizQuestion().getId(),
                answer.getQuizChoice().getId(),
                answer.getId(),
                answer.isCorrect(),
                answer.getSubmittedAt()
        );
    }
}
