package com.cygnus.iptn.inquiry.controller.request_form;

import com.cygnus.iptn.inquiry.entity.InquiryStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateInquiryStatusRequestForm(

        @NotNull(message = "변경할 상태값은 필수입니다.")
        InquiryStatus status
) {
}