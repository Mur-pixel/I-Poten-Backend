package com.cygnus.iptn.inquiry.controller.response_form;

import com.cygnus.iptn.inquiry.entity.InquiryStatus;
import com.cygnus.iptn.inquiry.entity.InquiryType;
import com.cygnus.iptn.inquiry.service.response.AdminInquirySummaryResponse;

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