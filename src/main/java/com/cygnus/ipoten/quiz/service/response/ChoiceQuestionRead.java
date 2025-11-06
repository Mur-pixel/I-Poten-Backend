package com.cygnus.ipoten.quiz.service.response;

import java.util.List;

public record ChoiceQuestionRead(
        Long id,
        String questionText,
        List<String> choices,
        Integer correctIndex, // 0-based
        String explanation
) {
}
