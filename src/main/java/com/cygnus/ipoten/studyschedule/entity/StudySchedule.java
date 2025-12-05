package com.cygnus.ipoten.studyschedule.entity;

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
public class StudySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_room_id", nullable = false)
    private StudyRoom studyRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_profile_id")
    private AccountProfile author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    private StudySchedule(StudyRoom studyRoom, AccountProfile author, String title, String description, LocalDateTime startTime, LocalDateTime endTime) {
        this.studyRoom = studyRoom;
        this.author = author;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static StudySchedule create(StudyRoom studyRoom, AccountProfile author, String title, String description, LocalDateTime startTime, LocalDateTime endTime) {
        return new StudySchedule(
                studyRoom,
                author,
                title,
                description,
                startTime,
                endTime
        );
    }

    public void update(String title, String description, LocalDateTime startTime, LocalDateTime endTime) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setAuthor(AccountProfile author){
        this.author = author;
    }
}