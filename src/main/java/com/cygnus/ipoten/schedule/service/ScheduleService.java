package com.cygnus.ipoten.schedule.service;

import com.cygnus.ipoten.schedule.service.request.CreateScheduleRequest;
import com.cygnus.ipoten.schedule.service.request.ListScheduleRequest;
import com.cygnus.ipoten.schedule.service.request.UpdateScheduleRequest;
import com.cygnus.ipoten.schedule.service.response.DeleteScheduleResponse;
import com.cygnus.ipoten.schedule.service.response.ScheduleListResponse;
import com.cygnus.ipoten.schedule.service.response.ScheduleResponse;

public interface ScheduleService {

    ScheduleResponse create(CreateScheduleRequest request);

    ScheduleListResponse list(ListScheduleRequest request);

    ScheduleResponse read(Long accountId, Long scheduleId);

    ScheduleResponse update(UpdateScheduleRequest request);

    DeleteScheduleResponse delete(Long accountId, Long scheduleId);
}
