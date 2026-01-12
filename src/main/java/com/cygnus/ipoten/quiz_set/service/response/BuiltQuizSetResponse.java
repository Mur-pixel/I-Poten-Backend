package com.cygnus.ipoten.quiz_set.service.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BuiltQuizSetResponse {
    private final Long quizSetId;
    private final List<Long> questionIds;
    private final String title;
    private final boolean isRandom;
    private final int totalQuestions;
}