package com.cygnus.ipoten.studyschedule.service;

import com.cygnus.ipoten.studyschedule.service.request.UpdateAttendanceRequest;
import com.cygnus.ipoten.studyschedule.service.response.CreateScheduleAttendanceResponse;
import com.cygnus.ipoten.studyschedule.service.response.ListAttendanceStatusResponse;

import java.util.List;

public interface ScheduleAttendanceService {

    CreateScheduleAttendanceResponse checkAttendance(Long studyScheduleId, Long accountProfileId);

    List<ListAttendanceStatusResponse> getAttendanceList(Long studyScheduleId, Long leaderId);

    void confirmAttendance(Long studyScheduleId, Long leaderId, List<UpdateAttendanceRequest> requests);

}