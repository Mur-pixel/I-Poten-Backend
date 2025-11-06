package com.cygnus.ipoten.userSchedule.controller.response;

import com.cygnus.ipoten.userSchedule.entity.UserSchedule;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserScheduleResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String Location;
    private final boolean allDay;
    private final String color;

    public UserScheduleResponse(UserSchedule schedule) {
        this.id = schedule.getId();
        this.title = schedule.getTitle();
        this.description = schedule.getDescription();
        this.startTime = schedule.getStartTime();
        this.endTime = schedule.getEndTime();
        this.Location = schedule.getLocation();
        this.allDay = schedule.isAllDay();
        this.color = schedule.getColor();
    }
}
