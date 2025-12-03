package com.cygnus.ipoten.quiz.entity;

import com.cygnus.ipoten.quiz.entity.enums.QuizSetType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Getter
@Entity
@Table(name = "quiz")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 퀴즈 제목(세트 묶음의 큰 카테고리) */
    @Column(nullable = false, length = 255)
    private String title;

    /** 퀴즈 세트 타입(CHOICE/OX/INITIALS/MIX) */
    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_set_type", nullable = false)
    private QuizSetType quizSetType;

    /** 생성일 */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** 수정일 */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Quiz() {}

    private Quiz(String title, QuizSetType quizSetType) {
        this.title = title;
        this.quizSetType = quizSetType;
    }

    public static Quiz create(String title, QuizSetType quizSetType) {
        return new Quiz(title, quizSetType);
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}