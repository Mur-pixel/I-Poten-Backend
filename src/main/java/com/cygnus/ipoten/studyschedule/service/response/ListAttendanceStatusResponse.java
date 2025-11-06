package com.cygnus.ipoten.studyschedule.service.response;

import com.cygnus.ipoten.studyschedule.entity.AttendanceStatus;
import com.cygnus.ipoten.studyschedule.entity.ScheduleAttendance;
import lombok.Getter;

@Getter
public class ListAttendanceStatusResponse {
    private final Long studyMemberId;
    private final String nickname;
    private final AttendanceStatus status;

    private ListAttendanceStatusResponse(ScheduleAttendance attendance) {
        this.studyMemberId = attendance.getStudyMember().getId();
        this.nickname = attendance.getStudyMember().getAccountProfile().getNickname();
        this.status = attendance.getStatus();
    }

    public static ListAttendanceStatusResponse from(ScheduleAttendance attendance) {
        return new ListAttendanceStatusResponse(attendance);
    }
}