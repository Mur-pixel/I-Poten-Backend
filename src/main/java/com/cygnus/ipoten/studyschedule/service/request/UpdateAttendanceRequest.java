package com.cygnus.ipoten.studyschedule.service.request;

import com.cygnus.ipoten.studyschedule.entity.AttendanceStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateAttendanceRequest {
    private final Long studyMemberId;
    private final AttendanceStatus status;
}
