package com.cygnus.ipoten.schedule.service.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DeleteScheduleResponse {

    private final Long scheduleId;
    private final boolean deleted;
}
