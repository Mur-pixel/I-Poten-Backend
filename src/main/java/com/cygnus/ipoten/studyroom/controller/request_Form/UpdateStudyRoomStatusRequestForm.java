package com.cygnus.ipoten.studyroom.controller.request_Form;

import com.cygnus.ipoten.studyroom.entity.StudyStatus;
import com.cygnus.ipoten.studyroom.service.request.UpdateStudyRoomStatusRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateStudyRoomStatusRequestForm {
    private final String status;

    public UpdateStudyRoomStatusRequest toServiceRequest() {
        return new UpdateStudyRoomStatusRequest(StudyStatus.valueOf(status.toUpperCase()));
    }
}
