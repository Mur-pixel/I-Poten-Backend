package com.cygnus.iptn.inquiry.controller.request_form;

import com.cygnus.iptn.inquiry.entity.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInquiryRequestForm(

        @NotNull(message = "문의 유형은 필수입니다.")
        InquiryType type,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(min = 2, max = 200, message = "제목은 2자 이상 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "문의 내용은 필수입니다.")
        @Size(min = 5, max = 5000, message = "문의 내용은 5자 이상 5000자 이하여야 합니다.")
        String content
) {
}
