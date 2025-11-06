package com.cygnus.ipoten.userSchedule.service;

import com.cygnus.ipoten.userSchedule.controller.request.UserScheduleRequest;
import com.cygnus.ipoten.userSchedule.entity.UserSchedule;
import com.cygnus.ipoten.userSchedule.repository.UserScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserScheduleServiceImpl implements UserScheduleService {

    private final UserScheduleRepository userScheduleRepository;

    // 일정 등록
    @Override
    public UserSchedule createUserSchedule(Long accountId, UserScheduleRequest request) {

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        LocalDateTime start;
        LocalDateTime end;

        // allDay 일정인 경우: 프론트에서 받은 날짜 범위 기준으로 00:00~23:59:59 보정
        if (request.isAllDay()) {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new IllegalArgumentException("StartTime and EndTime are required for all-day events");
            }

            LocalDate startDate = request.getStartTime().toLocalDate();
            LocalDate endDate = request.getEndTime().toLocalDate();

            start = startDate.atStartOfDay();
            end = endDate.atTime(LocalTime.of(23, 59, 59));
        }
        // 일반 일정은 그대로 저장
        else {
            if (request.getStartTime() == null || request.getEndTime() == null) {
                throw new IllegalArgumentException("StartTime and EndTime are required for non-allDay events");
            }
            start = request.getStartTime();
            end = request.getEndTime();
        }

        UserSchedule schedule = UserSchedule.builder()
                .accountId(accountId)
                .title(request.getTitle())
                .description(request.getDescription())
                .startTime(start)
                .endTime(end)
                .location(request.getLocation())
                .allDay(request.isAllDay())
                .color(request.getColor() != null ? request.getColor() : "#3b82f6")
                .build();

        return userScheduleRepository.save(schedule);
    }

    // 전체 일정 조회
    @Override
    @Transactional(readOnly = true)
    public List<UserSchedule> getUserSchedules(Long accountId) {
        return userScheduleRepository.findAllByAccountId(accountId);
    }

    // 특정 일정 상세 조회
    @Override
    @Transactional(readOnly = true)
    public UserSchedule getUserScheduleById(Long accountId, Long id) {
        UserSchedule schedule = userScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 존재하지 않습니다."));

        if (!schedule.getAccountId().equals(accountId)) {
            throw new IllegalStateException("본인 일정만 조회할 수 있습니다.");
        }

        return schedule;
    }

    // 일정 삭제
    @Override
    public void deleteUserSchedule(Long accountId, Long id) {
        UserSchedule schedule = userScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 존재하지 않습니다."));

        if (!schedule.getAccountId().equals(accountId)) {
            throw new IllegalStateException("본인 일정만 삭제할 수 있습니다.");
        }

        userScheduleRepository.delete(schedule);
    }

    // 일정 수정
    @Override
    public UserSchedule updateUserSchedule(Long accountId, Long id, UserScheduleRequest request){
        UserSchedule schedule = userScheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정이 존재하지 않습니다."));

        if (!schedule.getAccountId().equals(accountId)) {
            throw new IllegalStateException("본인 일정만 수정할 수 있습니다.");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            schedule.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            schedule.setDescription(request.getDescription());
        }

        // 수정 시에도 allDay 로직 동일하게 적용
        if (request.isAllDay()) {
            if (request.getStartTime() != null && request.getEndTime() != null) {
                LocalDate startDate = request.getStartTime().toLocalDate();
                LocalDate endDate = request.getEndTime().toLocalDate();
                schedule.setStartTime(startDate.atStartOfDay());
                schedule.setEndTime(endDate.atTime(LocalTime.of(23, 59, 59)));
            }
        } else {
            if (request.getStartTime() != null) schedule.setStartTime(request.getStartTime());
            if (request.getEndTime() != null) schedule.setEndTime(request.getEndTime());
        }

        if (request.getLocation() != null) {
            schedule.setLocation(request.getLocation());
        }

        if (request.getColor() != null) {
            schedule.setColor(request.getColor());
        }

        schedule.setAllDay(request.isAllDay());

        return userScheduleRepository.save(schedule);
    }
}