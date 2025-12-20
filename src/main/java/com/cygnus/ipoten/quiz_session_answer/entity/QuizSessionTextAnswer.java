package com.cygnus.ipoten.quiz_session_answer.entity;

import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "quiz_session_text_answer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_session_question", columnNames = {"quiz_session_id", "quiz_question_id"})
        },
        indexes = {
                @Index(name = "idx_qsta_session", columnList = "quiz_session_id"),
                @Index(name = "idx_qsta_question", columnList = "quiz_question_id")
        }
)
public class QuizSessionTextAnswer {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_session_id", nullable = false)
    private QuizSession quizSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    @Column(name = "submitted_text", nullable = false, length = 255)
    private String submittedText;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public QuizSessionTextAnswer(QuizSession s, QuizQuestion q, String submittedText, boolean isCorrect, Instant submittedAt) {
        this.quizSession = s;
        this.quizQuestion = q;
        this.submittedText = submittedText;
        this.isCorrect = isCorrect;
        this.submittedAt = submittedAt;
    }

    public static QuizSessionTextAnswer create(QuizSession s, QuizQuestion q, String submittedText, boolean isCorrect, Instant submittedAt) {
        return new QuizSessionTextAnswer(s, q, submittedText, isCorrect, submittedAt);
    }
}
