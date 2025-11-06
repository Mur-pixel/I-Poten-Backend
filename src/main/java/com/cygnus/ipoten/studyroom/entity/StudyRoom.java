package com.cygnus.ipoten.studyroom.entity;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.studyschedule.entity.StudySchedule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_profile_id")
    private AccountProfile host;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer maxMembers;

    @Enumerated(EnumType.STRING)        // DB에서는 Enum의 이름이 문자열로 저장됨
    @Column(nullable = false)
    private StudyStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyLocation location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyLevel studyLevel;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recruiting_roles", joinColumns = @JoinColumn(name = "study_room_id"))
    @Column(name = "role_name")
    private Set<String> recruitingRoles = new HashSet<>(); // 👈 List -> Set으로 변경

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "skill_stack", joinColumns = @JoinColumn(name = "study_room_id"))
    @Column(name = "skill_name")
    private Set<String> skillStack = new HashSet<>(); // 👈 List -> Set으로 변경

    @OneToMany(mappedBy = "studyRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudyMember> studyMembers = new ArrayList<>(); // 현재 멤버 목록

    // ✅ [추가] StudyRoom이 StudySchedule 목록을 가지도록 관계 설정
    @OneToMany(mappedBy = "studyRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudySchedule> schedules = new ArrayList<>();

    // ✅ [추가] StudyRoom이 Announcement 목록을 가지도록 관계 설정
    @OneToMany(mappedBy = "studyRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Announcement> announcements = new ArrayList<>();

    // ✅ [추가] StudyRoom이 InterviewChannel 목록을 가지도록 관계 설정
    @OneToMany(mappedBy = "studyRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewChannel> interviewChannels = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    // 생성자 추가
    private StudyRoom(AccountProfile host, String title, String description, Integer maxMembers, StudyLocation location, StudyLevel studyLevel, Set<String> recruitingRoles, Set<String> skillStack) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.maxMembers = maxMembers;
        this.status = StudyStatus.RECRUITING;
        this.location = location;
        this.studyLevel = studyLevel;
        this.recruitingRoles = recruitingRoles;
        this.skillStack = skillStack;
    }

    // create 정적 팩토리 메서드 추가
    public static StudyRoom create(AccountProfile host, String title, String description, Integer maxMembers, StudyLocation location, StudyLevel studyLevel, Set<String> recruitingRoles, Set<String> skillStack) {
        return new StudyRoom(host, title, description, maxMembers, location, studyLevel, recruitingRoles, skillStack);
    }

    // update 정적 팩토리 매서드 추가
    public void update(String title, String description, Integer maxMembers, StudyLocation location, StudyLevel studyLevel, Set<String> recruitingRoles, Set<String> skillStack) {
        this.title = title;
        this.description = description;
        this.maxMembers = maxMembers;
        this.location = location;
        this.studyLevel = studyLevel;
        this.recruitingRoles = recruitingRoles;
        this.skillStack = skillStack;
    }

    // status 변경을 위한 전용 매서드
    public void updateStatus(StudyStatus status) {
        this.status = status;
    }

    // 양방향 관계 편의 매서드
    public void addStudyMember(StudyMember member){
        this.studyMembers.add(member);
    }

    // 양방향 관계 편의 메서드 (멤버 제거)
    public void removeStudyMember(StudyMember member) {
        this.studyMembers.remove(member);
    }

    public void updateHost(AccountProfile newHost) {
        this.host = newHost;
    }
}