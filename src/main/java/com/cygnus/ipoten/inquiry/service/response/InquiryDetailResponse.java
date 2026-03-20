package com.cygnus.ipoten.inquiry.service.response;

import com.cygnus.ipoten.inquiry.entity.Inquiry;
import com.cygnus.ipoten.inquiry.entity.InquiryStatus;
import com.cygnus.ipoten.inquiry.entity.InquiryType;

import java.time.LocalDateTime;

public record InquiryDetailResponse(
        Long id,
        Long accountId,
        InquiryType type,
        String title,
        String content,
        InquiryStatus status,
        String answerContent,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InquiryDetailResponse from(Inquiry inquiry) {
        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getAccount().getId(),
                inquiry.getType(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getAnswerContent(),
                inquiry.getAnsweredAt(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt()
        );
    }
}
