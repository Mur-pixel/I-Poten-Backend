package com.cygnus.ipoten.studyApplication.controller.response_form;

import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import com.cygnus.ipoten.studyApplication.service.response.ApplicationForHostResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ApplicationForHostResponseForm {
    private final Long applicationId;
    private final Long applicantId;
    private final String applicantNickname;
    private final String message;
    private final ApplicationStatus status;
    private final LocalDateTime appliedAt;

    public static ApplicationForHostResponseForm from(ApplicationForHostResponse response) {
        return new ApplicationForHostResponseForm(
                response.getApplicationId(),
                response.getApplicantId(),
                response.getApplicantNickname(),
                response.getMessage(),
                response.getStatus(),
                response.getAppliedAt()
        );
    }
}