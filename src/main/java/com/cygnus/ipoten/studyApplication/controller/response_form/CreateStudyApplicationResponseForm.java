package com.cygnus.ipoten.studyApplication.controller.response_form;

import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import com.cygnus.ipoten.studyApplication.service.response.CreateStudyApplicationResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CreateStudyApplicationResponseForm {
    private final Long applicationId;
    private final ApplicationStatus status;
    private final LocalDateTime appliedAt;

    public static CreateStudyApplicationResponseForm from(CreateStudyApplicationResponse response) {
        return new CreateStudyApplicationResponseForm(
                response.getApplicationId(),
                response.getStatus(),
                response.getAppliedAt()
        );
    }
}
