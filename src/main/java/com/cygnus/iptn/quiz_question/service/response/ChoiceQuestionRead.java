package com.cygnus.iptn.quiz_question.service.response;

import java.util.List;

public record ChoiceQuestionRead(
        Long id,
        String questionText,
        List<String> choices,
        Integer correctIndex, // 0-based
        String explanation
) {
}
