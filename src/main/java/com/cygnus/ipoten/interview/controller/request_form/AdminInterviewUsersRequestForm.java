package com.cygnus.ipoten.interview.controller.request_form;

import com.cygnus.ipoten.interview.entity.InterviewType;

import java.time.LocalDate;
import java.util.List;

public record AdminInterviewUsersRequestForm(
        Integer pageSize,
        Long lastAccountId,
        String q,
        LocalDate startDate,
        LocalDate endDate,
        List<InterviewType> types
) {
    public int resolvedPageSize() {
        if (pageSize == null || pageSize <= 0) return 30;
        return Math.min(pageSize, 100);
    }
}
