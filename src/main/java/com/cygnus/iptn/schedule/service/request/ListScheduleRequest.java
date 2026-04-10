package com.cygnus.iptn.schedule.service.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class ListScheduleRequest {

    private final Long accountId;
    private final Instant from;
    private final Instant to;

    public boolean hasRange() {
        return from != null || to != null;
    }
}
