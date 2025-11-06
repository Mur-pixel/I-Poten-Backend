package com.cygnus.ipoten.studyschedule.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.studyschedule.service.StudyScheduleService;
import com.cygnus.ipoten.studyschedule.service.response.ListUserStudyScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my-study")
public class UserStudyScheduleController {

    private final StudyScheduleService studyScheduleService;
    private final RedisCacheService redisCacheService;

    /**
     *  로그인한 사용자가 속한 스터디룸의 모든 일정 조회
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<ListUserStudyScheduleResponse>> getAllSchedulesByUser(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        List<ListUserStudyScheduleResponse> schedules =
                studyScheduleService.findAllSchedulesByUser(accountId);
        return ResponseEntity.ok(schedules);
    }
}