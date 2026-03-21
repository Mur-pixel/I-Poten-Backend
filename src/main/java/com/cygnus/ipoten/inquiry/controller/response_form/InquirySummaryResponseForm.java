package com.cygnus.ipoten.inquiry.controller.response_form;

import com.cygnus.ipoten.inquiry.entity.InquiryStatus;
import com.cygnus.ipoten.inquiry.entity.InquiryType;
import com.cygnus.ipoten.inquiry.service.response.InquirySummaryResponse;

import java.time.LocalDateTime;

public record InquirySummaryResponseForm(
        Long id,
        InquiryType type,
        String title,
        InquiryStatus status,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
    public static InquirySummaryResponseForm from(InquirySummaryResponse response) {
        return new InquirySummaryResponseForm(
                response.id(),
                response.type(),
                response.title(),
                response.status(),
                response.answeredAt(),
                response.createdAt()
        );
    }
}