package com.cygnus.ipoten.custom_term_recommendation.controller.request_form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachJobGroupRequestForm {

    @NotBlank
    private String jobKey;

    public String getJobKey() {
        return jobKey;
    }
}
