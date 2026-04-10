package com.cygnus.ipoten.schedule.controller.request_form;

import com.cygnus.ipoten.schedule.service.request.CreateScheduleRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class CreateScheduleRequestForm {

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

    public CreateScheduleRequest toRequest(Long accountId) {
        return new CreateScheduleRequest(accountId, title, memo, startAt, endAt, Boolean.TRUE.equals(allDay));
    }
}
