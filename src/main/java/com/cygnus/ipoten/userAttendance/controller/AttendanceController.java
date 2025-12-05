package com.cygnus.ipoten.userAttendance.controller;

import com.cygnus.ipoten.userAttendance.controller.response.AttendanceMarkResponse;
import com.cygnus.ipoten.userAttendance.service.AttendanceService;
import com.cygnus.ipoten.userDashboard.service.TokenAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final TokenAccountService tokenAccountService;
    private final AttendanceService attendanceService;

    /**
     * 오늘자 출석 기록 → 새로 찍혔는지 여부 반환
     */
    @PostMapping("/mark")
    public ResponseEntity<AttendanceMarkResponse> mark(
            @CookieValue(name = "userToken", required = false) String userToken
    ){
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        boolean created = attendanceService.markLogin(accountId);

        // 오늘 처음 찍혔으면 201 Created, 이미 있으면 200 OK
        return ResponseEntity
                .status(created ? 201 : 200)
                .body(new AttendanceMarkResponse(created));
    }
}