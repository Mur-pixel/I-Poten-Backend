package com.cygnus.iptn.inquiry.service.request;

import com.cygnus.iptn.inquiry.entity.InquiryType;

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