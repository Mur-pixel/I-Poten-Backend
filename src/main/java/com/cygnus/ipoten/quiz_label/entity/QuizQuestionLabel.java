package com.cygnus.ipoten.quiz_label.entity;

import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "quiz_question_label",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_qq_label",
                columnNames = {"quiz_question_id", "label_id"}
        ),
        indexes = {
                @Index(name = "idx_qq_label_question", columnList = "quiz_question_id"),
                @Index(name = "idx_qq_label", columnList = "label_id")
        }
)
public class QuizQuestionLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "label_id", nullable = false)
    private QuizLabel quizLabel;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    private QuizQuestionLabel(QuizQuestion quizQuestion, QuizLabel quizLabel) {
        if (quizQuestion == null) {
            throw new IllegalArgumentException("quizQuestion cannot be null");
        }

        if (quizLabel == null) {
            throw new IllegalArgumentException("quizLabel cannot be null");
        }

        this.quizQuestion = quizQuestion;
        this.quizLabel = quizLabel;
    }

    public static QuizQuestionLabel create(QuizQuestion quizQuestion, QuizLabel quizLabel) {
        return new QuizQuestionLabel(quizQuestion, quizLabel);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
