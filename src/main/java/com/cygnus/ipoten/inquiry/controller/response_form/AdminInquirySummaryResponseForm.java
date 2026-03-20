package com.cygnus.ipoten.inquiry.controller.response_form;

import com.cygnus.ipoten.inquiry.entity.InquiryStatus;
import com.cygnus.ipoten.inquiry.entity.InquiryType;
import com.cygnus.ipoten.inquiry.service.response.AdminInquirySummaryResponse;

import java.time.LocalDateTime;

public record AdminInquirySummaryResponseForm(
        Long id,
        Long accountId,
        InquiryType type,
        String title,
        InquiryStatus status,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
    public static AdminInquirySummaryResponseForm from(AdminInquirySummaryResponse response) {
        return new AdminInquirySummaryResponseForm(
                response.id(),
                response.accountId(),
                response.type(),
                response.title(),
                response.status(),
                response.createdAt(),
                response.answeredAt()
        );
    }
}