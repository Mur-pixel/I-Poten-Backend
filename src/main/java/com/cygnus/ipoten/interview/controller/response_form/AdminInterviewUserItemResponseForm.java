package com.cygnus.ipoten.interview.controller.response_form;

import com.cygnus.ipoten.interview.entity.InterviewType;

import java.util.List;

public record AdminInterviewUserItemResponseForm(
        Long id,
        String email,
        String nickname,
        String joinedAt,
        String role,
        long interviewCount,
        String lastInterviewAt,
        List<InterviewType> interviewTypes
) {
}
