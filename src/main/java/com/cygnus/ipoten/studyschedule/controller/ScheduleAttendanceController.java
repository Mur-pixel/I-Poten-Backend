package com.cygnus.ipoten.studyschedule.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.studyschedule.service.ScheduleAttendanceService;
import com.cygnus.ipoten.studyschedule.service.request.UpdateAttendanceRequest;
import com.cygnus.ipoten.studyschedule.service.response.CreateScheduleAttendanceResponse;
import com.cygnus.ipoten.studyschedule.service.response.ListAttendanceStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules/{scheduleId}/attendance")
public class ScheduleAttendanceController {

    private final ScheduleAttendanceService scheduleAttendanceService;
    private final RedisCacheService redisCacheService;

    // 멤버가 특정 일정에 출석 체크
    @PostMapping
    public ResponseEntity<CreateScheduleAttendanceResponse> checkAttendance(
            @PathVariable Long scheduleId,
            @CookieValue(name = "userToken",  required = false) String userToken) {

        Long accountProfileId = redisCacheService.getValueByKey(userToken, Long.class);

        CreateScheduleAttendanceResponse response = scheduleAttendanceService.checkAttendance(scheduleId, accountProfileId);

        // 생성(Create)의 의미로 201 Created 상태 코드 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ListAttendanceStatusResponse>> getAttendanceList(
            @PathVariable Long scheduleId,
            @CookieValue(name = "userToken",  required = false) String userToken) {

        Long leaderId = redisCacheService.getValueByKey(userToken, Long.class);

        List<ListAttendanceStatusResponse> response = scheduleAttendanceService.getAttendanceList(scheduleId, leaderId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<Void> confirmAttendance(
            @PathVariable Long scheduleId,
            @CookieValue(name = "userToken",   required = false) String userToken,
            @RequestBody List<UpdateAttendanceRequest> requests) {

        Long leaderId = redisCacheService.getValueByKey(userToken, Long.class);

        scheduleAttendanceService.confirmAttendance(scheduleId, leaderId, requests);

        return ResponseEntity.ok().build();
    }
}