package com.cygnus.ipoten.studyschedule.controller.response_form;

import com.cygnus.ipoten.studyschedule.service.response.UpdateStudyScheduleResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class UpdateStudyScheduleResponseForm {
    private final Long id;
    private final Long authorId;
    private final String authorNickname;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public static UpdateStudyScheduleResponseForm from(UpdateStudyScheduleResponse response){
        return new UpdateStudyScheduleResponseForm(
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