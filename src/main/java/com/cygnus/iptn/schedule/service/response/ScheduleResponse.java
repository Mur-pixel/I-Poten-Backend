package com.cygnus.iptn.schedule.service.response;

import com.cygnus.iptn.schedule.entity.Schedule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class ScheduleResponse {

    private final Long id;
    private final String title;
    private final String memo;
    private final Instant startAt;
    private final Instant endAt;
    private final boolean allDay;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getMemo(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.isAllDay(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
