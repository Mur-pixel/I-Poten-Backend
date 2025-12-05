package com.cygnus.ipoten.userAttendance.service;

import com.cygnus.ipoten.userDashboard.controller.response.AttendanceRateResponse;

public interface AttendanceService {
    boolean markLogin(Long accountId);
    AttendanceRateResponse getThisMonthRate(Long accountId);
    int getConsecutiveDays(Long accountId);
}
