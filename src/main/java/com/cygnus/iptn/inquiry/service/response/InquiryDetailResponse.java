package com.cygnus.iptn.inquiry.service.response;

import com.cygnus.iptn.inquiry.entity.Inquiry;
import com.cygnus.iptn.inquiry.entity.InquiryStatus;
import com.cygnus.iptn.inquiry.entity.InquiryType;

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
