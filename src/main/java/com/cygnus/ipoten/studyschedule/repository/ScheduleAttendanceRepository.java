package com.cygnus.ipoten.studyschedule.repository;

import com.cygnus.ipoten.studyschedule.entity.ScheduleAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleAttendanceRepository extends JpaRepository<ScheduleAttendance, Long> {

    // 특정 일정의 모든 참석 정보를 조회
    List<ScheduleAttendance> findAllByStudyScheduleId(Long studyScheduleId);

    // 특정 일정에 특정 멤버의 참석 정보가 있는지 확인
    Optional<ScheduleAttendance> findByStudyScheduleIdAndStudyMemberId(Long studyScheduleId, Long studyMemberId);
}