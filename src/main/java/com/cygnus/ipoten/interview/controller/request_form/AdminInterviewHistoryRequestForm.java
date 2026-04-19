package com.cygnus.ipoten.interview.controller.request_form;

import com.cygnus.ipoten.interview.entity.InterviewType;

import java.time.LocalDate;
import java.util.List;

public record AdminInterviewHistoryRequestForm(
        Integer pageSize,
        Long lastInterviewId,
        LocalDate startDate,
        LocalDate endDate,
        List<InterviewType> types
) {
    public int resolvedPageSize() {
        if (pageSize == null || pageSize <= 0) return 50;
        return Math.min(pageSize, 100);
    }
}
