package com.cygnus.iptn.schedule.service.request;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.schedule.entity.Schedule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class CreateScheduleRequest {

    private final Long accountId;
    private final String title;
    private final String memo;
    private final Instant startAt;
    private final Instant endAt;
    private final boolean allDay;

    public Schedule toSchedule() {
        return new Schedule(
                new Account(accountId),
                title.trim(),
                memo.trim(),
                startAt,
                endAt,
                allDay
        );
    }
}
