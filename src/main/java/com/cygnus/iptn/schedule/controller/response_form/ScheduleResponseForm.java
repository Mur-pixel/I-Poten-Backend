package com.cygnus.iptn.schedule.controller.response_form;

import com.cygnus.iptn.schedule.service.response.ScheduleResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class ScheduleResponseForm {

    private final Long id;
    private final String title;
    private final String memo;
    private final Instant startAt;
    private final Instant endAt;
    private final boolean allDay;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ScheduleResponseForm from(ScheduleResponse response) {
        return new ScheduleResponseForm(
                response.getId(),
                response.getTitle(),
                response.getMemo(),
                response.getStartAt(),
                response.getEndAt(),
                response.isAllDay(),
                response.getCreatedAt(),
                response.getUpdatedAt()
        );
    }
}
