package com.cygnus.ipoten.studyschedule.service;

import com.cygnus.ipoten.studyroom.entity.StudyMember;
import com.cygnus.ipoten.studyroom.repository.StudyMemberRepository;
import com.cygnus.ipoten.studyschedule.entity.ScheduleAttendance;
import com.cygnus.ipoten.studyschedule.entity.StudySchedule;
import com.cygnus.ipoten.studyschedule.repository.ScheduleAttendanceRepository;
import com.cygnus.ipoten.studyschedule.repository.StudyScheduleRepository;
import com.cygnus.ipoten.studyschedule.service.request.UpdateAttendanceRequest;
import com.cygnus.ipoten.studyschedule.service.response.CreateScheduleAttendanceResponse;
import com.cygnus.ipoten.studyschedule.service.response.ListAttendanceStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleAttendanceServiceImpl implements ScheduleAttendanceService {

    private final ScheduleAttendanceRepository scheduleAttendanceRepository;
    private final StudyScheduleRepository studyScheduleRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Override
    public CreateScheduleAttendanceResponse checkAttendance(Long studyScheduleId, Long accountProfileId) {
        // 1. 요청받은 일정(Schedule) 정보 조회
        StudySchedule schedule = studyScheduleRepository.findById(studyScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        // 2. 해당 일정의 스터디룸 ID와 요청한 사용자의 ID로 스터디 멤버 정보 조회
        Long studyRoomId = schedule.getStudyRoom().getId();
        StudyMember member = studyMemberRepository.findByStudyRoomIdAndAccountProfileId(studyRoomId, accountProfileId)
                .orElseThrow(() -> new IllegalStateException("해당 스터디의 멤버가 아닙니다."));

        // 3. 이미 출석 정보가 있는지 확인 (중복 생성 방지)
        return scheduleAttendanceRepository.findByStudyScheduleIdAndStudyMemberId(studyScheduleId, member.getId())
                .map(CreateScheduleAttendanceResponse::from) // 이미 있다면, 기존 정보를 그대로 반환
                .orElseGet(() -> { // 없다면, 새로 생성 후 반환
                    ScheduleAttendance newAttendance = ScheduleAttendance.create(schedule, member);
                    ScheduleAttendance savedAttendance = scheduleAttendanceRepository.save(newAttendance);
                    return CreateScheduleAttendanceResponse.from(savedAttendance);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListAttendanceStatusResponse> getAttendanceList(Long studyScheduleId, Long leaderId) {
        // 1. 일정 정보 조회
        StudySchedule schedule = studyScheduleRepository.findById(studyScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        // 2. 요청한 사용자가 해당 스터디의 모임장인지 권한 확인
        if (!schedule.getStudyRoom().getHost().getId().equals(leaderId)) {
            throw new IllegalStateException("출석부를 조회할 권한이 없습니다.");
        }

        // 3. 해당 일정의 모든 출석 정보 조회
        List<ScheduleAttendance> attendances = scheduleAttendanceRepository.findAllByStudyScheduleId(studyScheduleId);

        // 4. 조회된 정보를 DTO 리스트로 변환하여 반환
        return attendances.stream()
                .map(ListAttendanceStatusResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public void confirmAttendance(Long studyScheduleId, Long leaderId, List<UpdateAttendanceRequest> requests) {
        // 1. 일정 정보 조회
        StudySchedule schedule = studyScheduleRepository.findById(studyScheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정입니다."));

        // 2. 요청한 사용자가 해당 스터디의 모임장인지 권한 확인
        if (!schedule.getStudyRoom().getHost().getId().equals(leaderId)) {
            throw new IllegalStateException("출석을 확정할 권한이 없습니다.");
        }

        // 3. 요청받은 각 멤버의 출석 상태를 업데이트
        for (UpdateAttendanceRequest request : requests) {
            ScheduleAttendance attendance = scheduleAttendanceRepository.findByStudyScheduleIdAndStudyMemberId(studyScheduleId, request.getStudyMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 출석 정보입니다. Member ID: " + request.getStudyMemberId()));

            attendance.updateStatus(request.getStatus());
        }
    }
}