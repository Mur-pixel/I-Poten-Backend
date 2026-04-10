package com.cygnus.iptn.inquiry.service.request;

import com.cygnus.iptn.inquiry.entity.InquiryStatus;

public record UpdateInquiryStatusRequest(
        Long inquiryId,
        InquiryStatus status
) {
    public static UpdateInquiryStatusRequest of(Long inquiryId, InquiryStatus status) {
        return new UpdateInquiryStatusRequest(inquiryId, status);
    }
}
