package com.cygnus.iptn.schedule.service.response;

import com.cygnus.iptn.schedule.entity.Schedule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ScheduleListResponse {

    private final List<ScheduleResponse> schedules;

    public static ScheduleListResponse from(List<Schedule> schedules) {
        return new ScheduleListResponse(
                schedules.stream()
                        .map(ScheduleResponse::from)
                        .toList()
        );
    }
}
