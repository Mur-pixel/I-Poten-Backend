package com.cygnus.ipoten.inquiry.service.request;

import com.cygnus.ipoten.inquiry.entity.InquiryStatus;

public record UpdateInquiryStatusRequest(
        Long inquiryId,
        InquiryStatus status
) {
    public static UpdateInquiryStatusRequest of(Long inquiryId, InquiryStatus status) {
        return new UpdateInquiryStatusRequest(inquiryId, status);
    }
}
