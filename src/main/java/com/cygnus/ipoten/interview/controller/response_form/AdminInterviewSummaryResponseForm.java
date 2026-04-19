package com.cygnus.ipoten.interview.controller.response_form;

public record AdminInterviewSummaryResponseForm(
        long totalCount,
        String lastInterviewAt,
        int averageScore
) {
}
