package com.cygnus.ipoten.studyroom.entity;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_room_id")
    private StudyRoom studyRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AccountProfile author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_pinned")
    private boolean pinned = false;

    public void togglePin() {
        this.pinned = !this.pinned;
    }

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Announcement(StudyRoom studyRoom, AccountProfile author, String title, String content) {
        this.studyRoom = studyRoom;
        this.author = author;
        this.title = title;
        this.content = content;
    }

    public static Announcement create(StudyRoom studyRoom, AccountProfile author, String title, String content) {
        return new Announcement(studyRoom, author, title, content);
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void setAuthor(AccountProfile author) {
        this.author = author;
    }
}