package com.cygnus.iptn.quiz_set.service.response;

public record ResolveQuizSetResult(
        Long quizSetId,
        String title,
        int totalQuestions
) {
}
