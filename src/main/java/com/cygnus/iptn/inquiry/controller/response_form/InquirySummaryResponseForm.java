package com.cygnus.iptn.inquiry.controller.response_form;

import com.cygnus.iptn.inquiry.entity.InquiryStatus;
import com.cygnus.iptn.inquiry.entity.InquiryType;
import com.cygnus.iptn.inquiry.service.response.InquirySummaryResponse;

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