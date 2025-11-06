package com.cygnus.ipoten.studyroom.entity;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.studyschedule.entity.ScheduleAttendance;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_room_id")
    private StudyRoom studyRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_profile_id")
    private AccountProfile accountProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyRole role;

    @OneToMany(mappedBy = "studyMember", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleAttendance> attendances = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime joinedAt;

    public StudyMember(StudyRoom studyRoom, AccountProfile accountProfile, StudyRole role) {
        this.studyRoom = studyRoom;
        this.accountProfile = accountProfile;
        this.role = role;
    }

    public static StudyMember create(StudyRoom studyRoom, AccountProfile accountProfile, StudyRole role) {
        return new StudyMember(studyRoom, accountProfile, role);
    }

    public void updateRole(StudyRole newRole){
        this.role = newRole;
    }
}