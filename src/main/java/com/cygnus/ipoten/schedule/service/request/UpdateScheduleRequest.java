package com.cygnus.ipoten.schedule.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class UpdateScheduleRequest {

    private final Long accountId;
    private final Long scheduleId;
    private final String title;
    private final String memo;
    private final Instant startAt;
    private final Instant endAt;
    private final boolean allDay;
}
