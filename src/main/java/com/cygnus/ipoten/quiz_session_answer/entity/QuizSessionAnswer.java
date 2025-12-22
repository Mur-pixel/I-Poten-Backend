package com.cygnus.ipoten.quiz_session_answer.entity;

import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import com.cygnus.ipoten.quiz_session.entity.QuizSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * SessionAnswer
 * 특정 QuizSession 내에서 사용자가 개별 문제(QuizQuestion)에 제출한 응답을 저장하는 엔티티.
 *
 * 특징:
 * - 특정 QuizSession 내에서 사용자가 특정 QuizQuestion에 제출한 "답안 스냅샷"을 저장한다.
 * - WrongNote 정책과 동일하게, 제출 답은 FK로 묶지 않고 "id/text 스냅샷"으로 보존한다.
 *
 * 저장 정책:
 * - 선택형(CHOICE/OX 등): submittedChoiceId
 * - 텍스트형(INITIALS/주관식 등): submittedText 사용
 *
 * 정답 여부(isCorrect)는 "제출 당시 채점 결과 스냅샷"으로 저장한다.
 * (문항/정답이 나중에 수정되어도 과거 세션 결과가 변하지 않도록)
 */
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "session_answer",
    uniqueConstraints = @UniqueConstraint(name = "uk_session_question", columnNames = {"session_id", "quiz_question_id"}),
    indexes = {
            @Index(name = "idx_sa_session", columnList = "session_id"),
            @Index(name = "idx_sa_question", columnList = "quiz_question_id"),
            @Index(name = "idx_sa_session_correct", columnList = "session_id, is_correct"),
            @Index(name = "idx_sa_submitted_at", columnList = "submitted_at")
    }
)
public class QuizSessionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // 응답 ID

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession quizSession;    // 소속 세션

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;  // 문제 ID

    @Column(name = "submitted_choice_id")
    private Long submittedChoiceId;

    @Column(name = "submitted_choice_text")
    private String submittedChoiceText;

    @Column(name = "submitted_text", length = 1000)
    private String submittedText;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;  // 응답 시각

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;  // 정답 여부

    @PrePersist
    void prePersist() {
        if (this.submittedAt == null) this.submittedAt = Instant.now();
    }

    public static QuizSessionAnswer forChoice(QuizSession session,
                                              QuizQuestion question,
                                              Long submittedChoiceId,
                                              String submittedChoiceText,
                                              boolean isCorrect,
                                              Instant submittedAt) {
        if (session == null) throw new IllegalArgumentException("session은 필수입니다.");
        if (question == null) throw new IllegalArgumentException("question은 필수입니다.");

        boolean hasId = (submittedChoiceId != null);
        boolean hasText = (submittedChoiceText != null && !submittedChoiceText.isBlank());
        if (!hasId && !hasText) {
            throw new IllegalArgumentException("선택형 제출은 submittedChoiceId 또는 submittedChoiceText 중 하나는 필요합니다.");
        }

        QuizSessionAnswer a = new QuizSessionAnswer();
        a.quizSession = session;
        a.quizQuestion = question;
        a.submittedChoiceId = submittedChoiceId;
        a.submittedChoiceText = hasText ? submittedChoiceText.trim() : null;
        a.submittedText = null;
        a.isCorrect = isCorrect;
        a.submittedAt = (submittedAt != null ? submittedAt : Instant.now());
        return a;
    }

    public static QuizSessionAnswer forText(QuizSession session,
                                            QuizQuestion question,
                                            String submittedText,
                                            boolean isCorrect,
                                            Instant submittedAt) {
        if (session == null) throw new IllegalArgumentException("session은 필수입니다.");
        if (question == null) throw new IllegalArgumentException("question은 필수입니다.");
        if (submittedText == null || submittedText.isBlank()) {
            throw new IllegalArgumentException("submittedText는 필수입니다.");
        }

        QuizSessionAnswer a = new QuizSessionAnswer();
        a.quizSession = session;
        a.quizQuestion = question;
        a.submittedChoiceId = null;
        a.submittedChoiceText = null;
        a.submittedText = submittedText.trim();
        a.isCorrect = isCorrect;
        a.submittedAt = (submittedAt != null ? submittedAt : Instant.now());
        return a;
    }
}