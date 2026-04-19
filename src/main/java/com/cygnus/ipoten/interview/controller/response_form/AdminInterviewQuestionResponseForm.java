package com.cygnus.ipoten.interview.controller.response_form;

import java.util.List;

public record AdminInterviewQuestionResponseForm(
        Long id,
        int order,
        String question,
        String answer,
        String feedback,
        String idealAnswer,
        int score,
        List<String> keywords
) {
}
