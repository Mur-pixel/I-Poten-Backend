package com.cygnus.ipoten.user_term.controller.request_form;

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
