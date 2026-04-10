package com.cygnus.iptn.inquiry.controller.response_form;

import com.cygnus.iptn.inquiry.entity.InquiryStatus;
import com.cygnus.iptn.inquiry.entity.InquiryType;
import com.cygnus.iptn.inquiry.service.response.InquiryDetailResponse;

import java.time.LocalDateTime;

public record InquiryDetailResponseForm(
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
    public static InquiryDetailResponseForm from(InquiryDetailResponse response) {
        return new InquiryDetailResponseForm(
                response.id(),
                response.accountId(),
                response.type(),
                response.title(),
                response.content(),
                response.status(),
                response.answerContent(),
                response.answeredAt(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}