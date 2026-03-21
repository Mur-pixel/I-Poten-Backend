package com.cygnus.ipoten.inquiry.service.response;

import com.cygnus.ipoten.inquiry.entity.Inquiry;
import com.cygnus.ipoten.inquiry.entity.InquiryStatus;
import com.cygnus.ipoten.inquiry.entity.InquiryType;

import java.time.LocalDateTime;

public record InquirySummaryResponse(
        Long id,
        InquiryType type,
        String title,
        InquiryStatus status,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
    public static InquirySummaryResponse from(Inquiry inquiry) {
        return new InquirySummaryResponse(
                inquiry.getId(),
                inquiry.getType(),
                inquiry.getTitle(),
                inquiry.getStatus(),
                inquiry.getAnsweredAt(),
                inquiry.getCreatedAt()
        );
    }
}
