package com.cygnus.ipoten.quiz_set.service.response;

public record ResolveQuizSetResult(
        Long quizSetId,
        String title,
        int totalQuestions
) {
}
