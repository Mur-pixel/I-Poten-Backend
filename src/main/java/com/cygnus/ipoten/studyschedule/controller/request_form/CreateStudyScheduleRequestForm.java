package com.cygnus.ipoten.studyschedule.controller.request_form;

import com.cygnus.ipoten.studyschedule.service.request.CreateStudyScheduleRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CreateStudyScheduleRequestForm {
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public CreateStudyScheduleRequest toServiceRequest(Long studyRoomId, Long authorId) {
        return new CreateStudyScheduleRequest(studyRoomId, authorId, title, description, startTime, endTime);
    }
}