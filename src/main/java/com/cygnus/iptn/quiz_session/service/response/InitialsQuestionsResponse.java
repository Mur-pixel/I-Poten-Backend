package com.cygnus.iptn.quiz_session.service.response;

import lombok.Value;

import java.util.List;

@Value
public class InitialsQuestionsResponse {
    Long sessionId;
    int total;
    List<QuestionItem> questions;

    @Value
    public static class QuestionItem {
        Long id;
        int order;
        String questionText;
        String answerText;
    }
}
