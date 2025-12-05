package com.cygnus.ipoten.studyschedule.controller.response_form;

import com.cygnus.ipoten.studyschedule.service.response.CreateStudyScheduleResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;


@Getter
@RequiredArgsConstructor
public class CreateStudyScheduleResponseForm {
    private final Long id;
    private final Long authorId;
    private final String authorNickname;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public static CreateStudyScheduleResponseForm from(CreateStudyScheduleResponse response) {
        return new CreateStudyScheduleResponseForm(
                response.getId(),
                response.getAuthorId(),
                response.getAuthorNickname(),
                response.getTitle(),
                response.getDescription(),
                response.getStartTime(),
                response.getEndTime()
        );
    }
}