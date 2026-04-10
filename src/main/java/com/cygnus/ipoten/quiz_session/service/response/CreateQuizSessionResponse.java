package com.cygnus.ipoten.quiz_session.service.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CreateQuizSessionResponse {
    private final Long quizSetId;
    private final List<Long> questionIds;

    public static CreateQuizSessionResponse of(Long quizSetId, List<Long> questionIds) {
        return new CreateQuizSessionResponse(quizSetId, questionIds);
    }
}
