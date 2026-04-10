package com.cygnus.iptn.inquiry.controller.request_form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerInquiryRequestForm(

        @NotBlank(message = "답변 내용은 필수입니다.")
        @Size(min = 2, max = 5000, message = "답변 내용은 2자 이상 5000자 이하여야 합니다.")
        String answerContent
) {
}