package com.cygnus.ipoten.inquiry.service.request;

import com.cygnus.ipoten.inquiry.entity.InquiryType;

public record CreateInquiryRequest(
        Long accountId,
        InquiryType type,
        String title,
        String content
) {
    public static CreateInquiryRequest of(
            Long accountId,
            InquiryType type,
            String title,
            String content
    ) {
        return new CreateInquiryRequest(
                accountId,
                type,
                title,
                content
        );
    }
}