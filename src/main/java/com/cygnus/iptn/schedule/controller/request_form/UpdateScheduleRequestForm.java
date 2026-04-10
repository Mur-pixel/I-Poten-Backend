package com.cygnus.iptn.schedule.controller.request_form;

import com.cygnus.iptn.schedule.service.request.UpdateScheduleRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class UpdateScheduleRequestForm {

    @NotBlank
    private String title;

    @NotNull
    private String memo;

    @NotNull
    private Instant startAt;

    @NotNull
    private Instant endAt;

    @NotNull
    private Boolean allDay;

    public UpdateScheduleRequest toRequest(Long accountId, Long scheduleId) {
        return new UpdateScheduleRequest(accountId, scheduleId, title, memo, startAt, endAt, Boolean.TRUE.equals(allDay));
    }
}
