package com.cygnus.iptn.interview.controller.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
public class InterviewAccountProjectRequest {

    private String projectName;
    private String projectDescription;

    public InterviewAccountProjectRequest(String projectName, String projectDescription) {
        this.projectName = projectName;
        this.projectDescription = projectDescription;
    }
}
