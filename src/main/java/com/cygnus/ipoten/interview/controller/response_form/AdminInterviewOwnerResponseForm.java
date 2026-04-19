package com.cygnus.ipoten.interview.controller.response_form;

public record AdminInterviewOwnerResponseForm(
        Long id,
        String email,
        String nickname,
        String role,
        String joinedAt
) {
}
