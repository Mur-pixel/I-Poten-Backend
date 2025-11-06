package com.cygnus.ipoten.userDashboard.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuizCompletionResponse {
    private long quizTotalCount;
    private long quizMonthlyCount;
}
