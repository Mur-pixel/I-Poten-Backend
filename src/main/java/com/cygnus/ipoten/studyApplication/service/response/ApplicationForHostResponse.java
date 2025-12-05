package com.cygnus.ipoten.studyApplication.service.response;

import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import com.cygnus.ipoten.studyApplication.entity.StudyApplication;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class ApplicationForHostResponse {
    private final Long applicationId;
    private final Long applicantId;
    private final String applicantNickname;
    private final String message;
    private final ApplicationStatus status;
    private final LocalDateTime appliedAt;

    public static ApplicationForHostResponse from(StudyApplication application) {
        return new ApplicationForHostResponse(
                application.getId(),
                application.getApplicant().getId(),
                application.getApplicant().getNickname(),
                application.getMessage(),
                application.getStatus(),
                application.getAppliedAt()
        );
    }
}
