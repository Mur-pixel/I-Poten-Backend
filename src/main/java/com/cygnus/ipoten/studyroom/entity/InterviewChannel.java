package com.cygnus.ipoten.studyroom.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_room_id", nullable = false)
    private StudyRoom studyRoom;

    @Column(nullable = false)
    private String channelName;

    @Column(columnDefinition = "TEXT")
    private String url;

    private InterviewChannel(StudyRoom studyRoom, String channelName) {
        this.studyRoom = studyRoom;
        this.channelName = channelName;
        this.url = ""; // 생성 시 URL은 비어있음
    }

    public static InterviewChannel create(StudyRoom studyRoom, String channelName) {
        return new InterviewChannel(studyRoom, channelName);
    }

        // URL 수정을 위한 메소드
        public void updateUrl(String url) {
            this.url = url;
        }
    }