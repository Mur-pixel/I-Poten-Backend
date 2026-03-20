package com.cygnus.ipoten.schedule.controller.response_form;

import com.cygnus.ipoten.schedule.service.response.ScheduleListResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ScheduleListResponseForm {

    private final List<ScheduleResponseForm> schedules;

    public static ScheduleListResponseForm from(ScheduleListResponse response) {
        return new ScheduleListResponseForm(
                response.getSchedules().stream()
                        .map(ScheduleResponseForm::from)
                        .toList()
        );
    }
}
