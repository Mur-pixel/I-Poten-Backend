package com.cygnus.iptn.quiz_set.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quiz_set")
public class QuizSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** 생성일 */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** 수정일 */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private QuizSet(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title은 필수입니다.");
        this.title = title;
    }

    public static QuizSet create(String title) {
        return new QuizSet(title);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
