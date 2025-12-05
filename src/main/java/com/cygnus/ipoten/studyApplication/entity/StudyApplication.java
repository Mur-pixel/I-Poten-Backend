package com.cygnus.ipoten.studyApplication.entity;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "study_application")          // 테이블명
public class StudyApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 하나의 스터디는 여러 개의 지원을 받음 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_room_id", nullable = false)
    private StudyRoom studyRoom;

    // 하나의 계정은 여러 스터디에 지원할 수 있음 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_profile_id", nullable = false)
    private AccountProfile applicant;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    // 엔터티가 생성될 때 자동으로 날짜를 설정함
    @PrePersist
    public void onPrePersist() {
        this.appliedAt = LocalDateTime.now();
    }

    // private 생성자로 외부에서 new 키워드를 통한 직접 생성을 제한함
    private StudyApplication(StudyRoom studyRoom, AccountProfile applicant, String message) {
        this.studyRoom = studyRoom;
        this.applicant = applicant;
        this.message = message;
        this.status = ApplicationStatus.PENDING;
    }

    // 정적 팩토리 메소드
    public static StudyApplication create(StudyRoom studyRoom, AccountProfile applicant, String message) {
        return new StudyApplication(studyRoom, applicant, message);
    }

    // 신청 상태를 변경하는 매서드
    public void updateStatus(ApplicationStatus newStatus) {
        this.status = newStatus;
    }
}