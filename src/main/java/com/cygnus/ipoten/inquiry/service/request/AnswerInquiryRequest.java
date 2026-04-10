package com.cygnus.ipoten.inquiry.service.request;

import com.cygnus.ipoten.inquiry.entity.InquiryStatus;

public record AnswerInquiryRequest(
        Long inquiryId,
        String answerContent
) {
    public static AnswerInquiryRequest of(Long inquiryId, String answerContent) {
        return new AnswerInquiryRequest(inquiryId, answerContent);
    }
}
