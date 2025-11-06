package com.cygnus.ipoten.studyschedule.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.studyschedule.controller.request_form.CreateStudyScheduleRequestForm;
import com.cygnus.ipoten.studyschedule.controller.request_form.UpdateStudyScheduleRequestForm;
import com.cygnus.ipoten.studyschedule.controller.response_form.CreateStudyScheduleResponseForm;
import com.cygnus.ipoten.studyschedule.controller.response_form.ListStudyScheduleResponseForm;
import com.cygnus.ipoten.studyschedule.controller.response_form.ReadStudyScheduleResponseForm;
import com.cygnus.ipoten.studyschedule.controller.response_form.UpdateStudyScheduleResponseForm;
import com.cygnus.ipoten.studyschedule.service.StudyScheduleService;
import com.cygnus.ipoten.studyroom.service.StudyRoomService;
import com.cygnus.ipoten.studyschedule.service.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study-rooms/{studyRoomId}/schedules")
public class StudyScheduleController {
    private final StudyScheduleService studyScheduleService;
    private final StudyRoomService studyRoomService;
    private final RedisCacheService redisCacheService;

    @PostMapping
    public ResponseEntity<CreateStudyScheduleResponseForm> createSchedule(
        @PathVariable Long studyRoomId,
        @CookieValue(name = "userToken", required = false) String userToken,
        @RequestBody CreateStudyScheduleRequestForm requestForm) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        studyRoomService.findUserRoleInStudyRoom(studyRoomId, currentUserId);

        var serviceRequest = requestForm.toServiceRequest(studyRoomId, currentUserId);
        CreateStudyScheduleResponse serviceResponse = studyScheduleService.createSchedule(serviceRequest);
        CreateStudyScheduleResponseForm responseForm = CreateStudyScheduleResponseForm.from(serviceResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseForm);
    }

    // 특정 스터디룸의 모든 일정 조회 API
    @GetMapping
    public ResponseEntity<List<ListStudyScheduleResponseForm>> getAllSchedules(
        @PathVariable Long studyRoomId) {
        List<ListStudyScheduleResponse> serviceResponse = studyScheduleService.findAllSchedules(studyRoomId);
        List<ListStudyScheduleResponseForm> responseForms = serviceResponse.stream()
                .map(ListStudyScheduleResponseForm::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseForms);
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ReadStudyScheduleResponseForm> getSchedule(
            @PathVariable Long studyRoomId,
            @PathVariable Long scheduleId) {

        ReadStudyScheduleResponse serviceResponse = studyScheduleService.findScheduleById(scheduleId);
        ReadStudyScheduleResponseForm responseForm = ReadStudyScheduleResponseForm.from(serviceResponse);

        return ResponseEntity.ok(responseForm);
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<UpdateStudyScheduleResponseForm> updateSchedule(
            @PathVariable Long studyRoomId,
            @PathVariable Long scheduleId,
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody UpdateStudyScheduleRequestForm requestForm) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        UpdateStudyScheduleResponse serviceResponse = studyScheduleService.updateSchedule(
                scheduleId, currentUserId, requestForm.toServiceRequest()
        );
        UpdateStudyScheduleResponseForm responseForm = UpdateStudyScheduleResponseForm.from(serviceResponse);

        return ResponseEntity.ok(responseForm);
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long studyRoomId,
            @PathVariable Long scheduleId,
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        studyScheduleService.deleteSchedule(scheduleId, currentUserId);

        return ResponseEntity.noContent().build();
    }
}