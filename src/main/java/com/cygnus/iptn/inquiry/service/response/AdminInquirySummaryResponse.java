package com.cygnus.iptn.inquiry.service.response;

import com.cygnus.iptn.inquiry.entity.Inquiry;
import com.cygnus.iptn.inquiry.entity.InquiryStatus;
import com.cygnus.iptn.inquiry.entity.InquiryType;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminInquirySummaryResponse(
        Long id,
        Long accountId,
        InquiryType type,
        String title,
        InquiryStatus status,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
    public static AdminInquirySummaryResponse from(Inquiry inquiry) {
        return new AdminInquirySummaryResponse(
                inquiry.getId(),
                inquiry.getAccount().getId(),
                inquiry.getType(),
                inquiry.getTitle(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getAnsweredAt()
        );
    }
}
