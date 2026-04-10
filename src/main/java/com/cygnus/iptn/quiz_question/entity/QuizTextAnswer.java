package com.cygnus.iptn.quiz_question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "quiz_text_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizTextAnswer {

    @Id
    @Column(name = "quiz_question_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "quiz_question_id")
    private QuizQuestion quizQuestion;

    @Column(name = "answer_text", nullable = false, length = 255)
    private String answerText;

    /** 텍스트 정답 생성 시간 */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** 텍스트 정답 업데이트 시간 */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    private QuizTextAnswer(QuizQuestion question, String answerText) {
        this.quizQuestion = question;
        this.answerText = answerText;
    }

    public static QuizTextAnswer create(QuizQuestion question, String answerText) {
        if (question == null) {
            throw new IllegalArgumentException("question은 필수입니다.");
        }
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("텍스트 정답은 필수입니다.");
        }
        return new QuizTextAnswer(question, answerText);
    }

    public void changeAnswerText(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("텍스트 정답은 필수입니다.");
        }
        this.answerText = answerText;
    }
}