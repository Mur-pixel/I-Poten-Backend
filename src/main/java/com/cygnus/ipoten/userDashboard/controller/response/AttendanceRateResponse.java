package com.cygnus.ipoten.userDashboard.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttendanceRateResponse {
    private double attendanceRate;
    private int attended;
    private int totalDays;
}
