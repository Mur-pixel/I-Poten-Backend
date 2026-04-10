package com.cygnus.iptn.schedule.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.schedule.entity.Schedule;
import com.cygnus.iptn.schedule.repository.ScheduleRepository;
import com.cygnus.iptn.schedule.service.request.CreateScheduleRequest;
import com.cygnus.iptn.schedule.service.request.ListScheduleRequest;
import com.cygnus.iptn.schedule.service.request.UpdateScheduleRequest;
import com.cygnus.iptn.schedule.service.response.DeleteScheduleResponse;
import com.cygnus.iptn.schedule.service.response.ScheduleListResponse;
import com.cygnus.iptn.schedule.service.response.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional
    public ScheduleResponse create(CreateScheduleRequest request) {
        validatePayload(request.getTitle(), request.getMemo(), request.getStartAt(), request.getEndAt());

        Schedule schedule = request.toSchedule();
        Schedule saved = scheduleRepository.save(schedule);
        return ScheduleResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleListResponse list(ListScheduleRequest request) {
        List<Schedule> schedules;

        if (request.hasRange()) {
            Instant from = request.getFrom();
            Instant to = request.getTo();
            if (from == null || to == null) {
                throw new IllegalArgumentException("from과 to는 함께 전달해야 합니다.");
            }
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
            }
            schedules = scheduleRepository
                    .findAllByAccount_IdAndEndAtGreaterThanEqualAndStartAtLessThanEqualOrderByStartAtAscIdAsc(
                            request.getAccountId(), from, to
                    );
        } else {
            schedules = scheduleRepository.findAllByAccount_IdOrderByStartAtAscIdAsc(request.getAccountId());
        }

        return ScheduleListResponse.from(schedules);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse read(Long accountId, Long scheduleId) {
        return ScheduleResponse.from(getOwnedSchedule(accountId, scheduleId));
    }

    @Override
    @Transactional
    public ScheduleResponse update(UpdateScheduleRequest request) {
        validatePayload(request.getTitle(), request.getMemo(), request.getStartAt(), request.getEndAt());

        Schedule schedule = getOwnedSchedule(request.getAccountId(), request.getScheduleId());
        schedule.update(
                request.getTitle().trim(),
                request.getMemo().trim(),
                request.getStartAt(),
                request.getEndAt(),
                request.isAllDay()
        );
        return ScheduleResponse.from(schedule);
    }

    @Override
    @Transactional
    public DeleteScheduleResponse delete(Long accountId, Long scheduleId) {
        Schedule schedule = getOwnedSchedule(accountId, scheduleId);
        scheduleRepository.delete(schedule);
        return new DeleteScheduleResponse(scheduleId, true);
    }

    private Schedule getOwnedSchedule(Long accountId, Long scheduleId) {
        return scheduleRepository.findByIdAndAccount_Id(scheduleId, accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다."));
    }

    private void validatePayload(String title, String memo, Instant startAt, Instant endAt) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("일정 제목은 비어 있을 수 없습니다.");
        }
        if (title.trim().length() > 100) {
            throw new IllegalArgumentException("일정 제목은 최대 100자입니다.");
        }
        if (memo == null) {
            throw new IllegalArgumentException("일정 메모는 null일 수 없습니다.");
        }
        if (memo.trim().length() > 2000) {
            throw new IllegalArgumentException("일정 메모는 최대 2000자입니다.");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("시작 시각과 종료 시각은 필수입니다.");
        }
        if (startAt.isAfter(endAt)) {
            throw new IllegalArgumentException("시작 시각은 종료 시각보다 늦을 수 없습니다.");
        }
    }
}
