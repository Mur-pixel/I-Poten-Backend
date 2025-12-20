package com.cygnus.ipoten.job_recommendation.controller.request_form;

import com.cygnus.ipoten.job.enums.JobRole;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachJobRoleRequestForm {

    @NotBlank
    private String jobRole;

    public JobRole toJobRole() {
        return JobRole.from(jobRole);
    }
}
