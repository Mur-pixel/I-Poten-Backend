package com.cygnus.ipoten.userSchedule.controller;

import com.cygnus.ipoten.userDashboard.service.TokenAccountService;
import com.cygnus.ipoten.userSchedule.controller.request.UserScheduleRequest;
import com.cygnus.ipoten.userSchedule.controller.response.UserScheduleResponse;
import com.cygnus.ipoten.userSchedule.entity.UserSchedule;
import com.cygnus.ipoten.userSchedule.service.UserScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-schedule")
public class UserScheduleController {

    private final UserScheduleService userScheduleService;
    private final TokenAccountService tokenAccountService;

    // 일정 등록
    @PostMapping("/create")
    public ResponseEntity<UserScheduleResponse> createUserSchedule(
            @CookieValue(name = "userToken") String userToken,
            @RequestBody UserScheduleRequest request
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        UserSchedule created = userScheduleService.createUserSchedule(accountId, request);
        return ResponseEntity.ok(new UserScheduleResponse(created));
    }

    // 전체 일정 조회
    @GetMapping("/list")
    public ResponseEntity<List<UserScheduleResponse>> getListUserSchedules(
            @CookieValue(name = "userToken") String userToken
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        List<UserScheduleResponse> responses = userScheduleService.getUserSchedules(accountId)
                .stream()
                .map(UserScheduleResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // 특정 일정 상세 보기
    @GetMapping("/get/{id}")
    public ResponseEntity<UserScheduleResponse> getUserScheduleDetail(
            @CookieValue(name = "userToken") String userToken,
            @PathVariable Long id
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        UserSchedule schedule = userScheduleService.getUserScheduleById(accountId, id);
        return ResponseEntity.ok(new UserScheduleResponse(schedule));
    }

    // 일정 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUserSchedule(
            @CookieValue(name = "userToken") String userToken,
            @PathVariable Long id
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        userScheduleService.deleteUserSchedule(accountId, id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // 일정 수정
    @PutMapping("/update/{id}")
    public ResponseEntity<UserScheduleResponse> updateUserSchedule(
            @CookieValue(name = "userToken") String userToken,
            @PathVariable Long id,
            @RequestBody UserScheduleRequest request
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        UserSchedule updated = userScheduleService.updateUserSchedule(accountId, id, request);
        return ResponseEntity.ok(new UserScheduleResponse(updated));
    }
}
