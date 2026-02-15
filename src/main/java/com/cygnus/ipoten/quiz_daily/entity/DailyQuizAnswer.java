package com.cygnus.ipoten.quiz_daily.entity;

import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "daily_quiz_answer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_daily_answer_session_question", columnNames = {"session_id", "question_id"})
        },
        indexes = {
                @Index(name = "idx_daily_answer_session", columnList = "session_id"),
                @Index(name = "idx_daily_answer_account", columnList = "account_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DailyQuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="session_id", nullable=false)
    private Long sessionId;

    @Column(name="question_id", nullable=false)
    private Long questionId;

    @Column(name="account_id", nullable=false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name="question_type", nullable=false, length=16)
    private QuestionType questionType;

    @Column(nullable=false)
    private boolean correct;

    // 사용자가 선택한 보기(CHOICE/OX)
    @Column(name="chosen_choice_id")
    private Long chosenChoiceId;

    // 정답 보기(CHOICE/OX) — “정답은 3번” 같은 안내를 고정하기 위함
    @Column(name="correct_choice_id")
    private Long correctChoiceId;

    // INITIALS 사용자가 입력한 값
    @Column(name="submitted_text", length=255)
    private String submittedText;

    // “정답 텍스트” (CHOICE/OX면 정답 보기 텍스트, INITIALS면 정답 텍스트)
    @Column(name="answer_text", columnDefinition = "TEXT")
    private String answerText;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name="answered_at", nullable=false)
    private LocalDateTime answeredAt;

    public static DailyQuizAnswer of(
            Long sessionId, Long questionId, Long accountId, QuestionType questionType,
            boolean correct, Long chosenChoiceId, Long correctChoiceId,
            String submittedText, String answerText, String explanation
    ) {
        return DailyQuizAnswer.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .accountId(accountId)
                .questionType(questionType)
                .correct(correct)
                .chosenChoiceId(chosenChoiceId)
                .correctChoiceId(correctChoiceId)
                .submittedText(submittedText)
                .answerText(answerText)
                .explanation(explanation)
                .answeredAt(LocalDateTime.now())
                .build();
    }

    public void updateChoiceAttempt(
            Long chosenChoiceId,
            boolean correct,
            Long correctChoiceId,
            String answerText,
            String explanation
    ) {
        this.chosenChoiceId = chosenChoiceId;
        this.correct = correct;
        this.correctChoiceId = correctChoiceId;
        this.answerText = answerText;
        this.explanation = explanation;
        this.answeredAt = LocalDateTime.now();
    }
}