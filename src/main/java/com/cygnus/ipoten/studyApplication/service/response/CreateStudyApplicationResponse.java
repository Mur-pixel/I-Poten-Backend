package com.cygnus.ipoten.studyApplication.service.response;

import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import com.cygnus.ipoten.studyApplication.entity.StudyApplication;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CreateStudyApplicationResponse {
    private final Long applicationId;
    private final ApplicationStatus status;
    private final LocalDateTime appliedAt;

    public static CreateStudyApplicationResponse from(StudyApplication studyApplication) {
        return new CreateStudyApplicationResponse(
                studyApplication.getId(),
                studyApplication.getStatus(),
                studyApplication.getAppliedAt()
        );
    }
}
