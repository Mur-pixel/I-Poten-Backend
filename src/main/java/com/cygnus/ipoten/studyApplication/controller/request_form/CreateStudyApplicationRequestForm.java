package com.cygnus.ipoten.studyApplication.controller.request_form;

import com.cygnus.ipoten.studyApplication.service.request.CreateStudyApplicationRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateStudyApplicationRequestForm {
    private final Long studyRoomId;
    private final String message;

    public CreateStudyApplicationRequest toServiceRequest(Long applicantId) {
        return new CreateStudyApplicationRequest(studyRoomId, applicantId, message);
    }
}
