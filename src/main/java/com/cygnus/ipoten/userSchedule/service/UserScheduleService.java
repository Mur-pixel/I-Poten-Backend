package com.cygnus.ipoten.userSchedule.service;

import com.cygnus.ipoten.userSchedule.controller.request.UserScheduleRequest;
import com.cygnus.ipoten.userSchedule.entity.UserSchedule;

import java.util.List;

public interface UserScheduleService {
    UserSchedule createUserSchedule(Long accountId, UserScheduleRequest request);
    List<UserSchedule> getUserSchedules(Long accountId);
    UserSchedule getUserScheduleById(Long accountId, Long id);
    void deleteUserSchedule(Long accountId, Long id);
    UserSchedule updateUserSchedule(Long accountId, Long id, UserScheduleRequest request);
}