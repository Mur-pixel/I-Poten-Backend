package com.cygnus.ipoten.quiz.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SubmitAnswerRequest {
    private final Long accountId;
    private final Long questionId;
    private final Long selectedChoiceId;
}
