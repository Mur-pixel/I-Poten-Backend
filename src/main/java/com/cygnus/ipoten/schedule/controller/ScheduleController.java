package com.cygnus.ipoten.schedule.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.schedule.controller.request_form.CreateScheduleRequestForm;
import com.cygnus.ipoten.schedule.controller.request_form.UpdateScheduleRequestForm;
import com.cygnus.ipoten.schedule.controller.response_form.DeleteScheduleResponseForm;
import com.cygnus.ipoten.schedule.controller.response_form.ScheduleListResponseForm;
import com.cygnus.ipoten.schedule.controller.response_form.ScheduleResponseForm;
import com.cygnus.ipoten.schedule.service.ScheduleService;
import com.cygnus.ipoten.schedule.service.request.ListScheduleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final RedisCacheService redisCacheService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleResponseForm create(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody @Valid CreateScheduleRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        return ScheduleResponseForm.from(scheduleService.create(requestForm.toRequest(accountId)));
    }

    @GetMapping
    public ScheduleListResponseForm list(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        Long accountId = resolveAccountId(userToken);
        return ScheduleListResponseForm.from(scheduleService.list(new ListScheduleRequest(accountId, from, to)));
    }

    @GetMapping("/{scheduleId}")
    public ScheduleResponseForm read(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long scheduleId
    ) {
        Long accountId = resolveAccountId(userToken);
        return ScheduleResponseForm.from(scheduleService.read(accountId, scheduleId));
    }

    @PutMapping("/{scheduleId}")
    public ScheduleResponseForm update(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long scheduleId,
            @RequestBody @Valid UpdateScheduleRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        return ScheduleResponseForm.from(scheduleService.update(requestForm.toRequest(accountId, scheduleId)));
    }

    @DeleteMapping("/{scheduleId}")
    public DeleteScheduleResponseForm delete(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long scheduleId
    ) {
        Long accountId = resolveAccountId(userToken);
        return DeleteScheduleResponseForm.from(scheduleService.delete(accountId, scheduleId));
    }

    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        if (accountId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return accountId;
    }
}
