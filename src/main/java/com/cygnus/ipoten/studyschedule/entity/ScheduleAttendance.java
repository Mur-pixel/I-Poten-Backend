package com.cygnus.ipoten.studyschedule.entity;

import com.cygnus.ipoten.studyroom.entity.StudyMember;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_schedule_id", nullable = false)
    private StudySchedule studySchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_member_id", nullable = false)
    private StudyMember studyMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    // 생성자
    private ScheduleAttendance(StudySchedule studySchedule, StudyMember studyMember) {
        this.studySchedule = studySchedule;
        this.studyMember = studyMember;
        this.status = AttendanceStatus.PENDING; // 생성 시 기본 상태는 PENDING
    }

    // 정적 팩토리 메소드
    public static ScheduleAttendance create(StudySchedule studySchedule, StudyMember studyMember) {
        return new ScheduleAttendance(studySchedule, studyMember);
    }

    // 모임장이 상태를 업데이트하기 위한 메소드
    public void updateStatus(AttendanceStatus status) {
        this.status = status;
    }
}
