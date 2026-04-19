package com.cygnus.ipoten.interview.controller.response_form;

import com.cygnus.ipoten.interview.entity.InterviewType;

import java.util.List;

public record AdminInterviewDetailResponseForm(
        Long id,
        InterviewType interviewType,
        String title,
        String role,
        String status,
        boolean finished,
        String createdAt,
        String completedAt,
        int durationMinutes,
        int questionCount,
        int totalScore,
        String summary,
        List<String> strengths,
        List<String> improvements,
        List<String> techKeywords,
        List<AdminInterviewQuestionResponseForm> questions,
        String pdfUrl,
        AdminInterviewOwnerResponseForm owner
) {
}
