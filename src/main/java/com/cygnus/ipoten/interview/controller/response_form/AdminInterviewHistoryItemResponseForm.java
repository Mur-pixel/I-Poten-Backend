package com.cygnus.ipoten.interview.controller.response_form;

import com.cygnus.ipoten.interview.entity.InterviewType;

public record AdminInterviewHistoryItemResponseForm(
        Long interviewId,
        InterviewType interviewType,
        String title,
        String role,
        String createdAt,
        String completedAt,
        int durationMinutes,
        int questionCount,
        int totalScore,
        String status,
        boolean finished
) {
}
