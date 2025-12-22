package com.cygnus.ipoten.quiz_wrongnote.entity;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "quiz_wrong_note",
        indexes = {
                @Index(
                        name = "idx_quiz_wrong_note_user_question_time",
                        columnList = "account_id, quiz_question_id, submitted_at"
                )
        }
)
public class QuizWrongNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 오답 히스토리 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion; // 틀린 문제

    /**
     * 객관식/ OX 등 "보기 선택형"에서 사용
     * - FK로 묶지 않고 id만 스냅샷으로 저장(보기 삭제/교체에도 오답노트 보존)
     */
    @Column(name = "submitted_choice_id")
    private Long submittedChoiceId;

    /**
     * 보기 텍스트 스냅샷(선택형에서 UI에 바로 표시 가능)
     */
    @Column(name = "submitted_choice_text", length = 500)
    private String submittedChoiceText;

    /** 주관식/초성 등 "텍스트 제출형"에서 사용 */
    @Column(name = "submitted_text", length = 1000)
    private String submittedText;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @PrePersist
    void prePersist() {
        if (this.submittedAt == null) this.submittedAt = Instant.now();
    }

    public static QuizWrongNote forChoice(Account account,
                                          QuizQuestion question,
                                          Long submittedChoiceId,
                                          String submittedChoiceText,
                                          Instant submittedAt) {
        if (account == null) throw new IllegalArgumentException("account는 필수입니다.");
        if (question == null) throw new IllegalArgumentException("question은 필수입니다.");
        if (submittedChoiceId == null && (submittedChoiceText == null || submittedChoiceText.isBlank())) {
            throw new IllegalArgumentException("선택형 오답은 submittedChoiceId 또는 submittedChoiceText 중 하나는 필요합니다.");
        }

        QuizWrongNote r = new QuizWrongNote();
        r.account = account;
        r.quizQuestion = question;
        r.submittedChoiceId = submittedChoiceId;
        r.submittedChoiceText = (submittedChoiceText == null ? null : submittedChoiceText.trim());
        r.submittedText = null;
        r.submittedAt = (submittedAt != null ? submittedAt : Instant.now());
        return r;
    }

    public static QuizWrongNote forText(Account account,
                                        QuizQuestion question,
                                        String submittedText,
                                        Instant submittedAt) {
        if (account == null) throw new IllegalArgumentException("account는 필수입니다.");
        if (question == null) throw new IllegalArgumentException("question은 필수입니다.");
        if (submittedText == null || submittedText.isBlank()) {
            throw new IllegalArgumentException("submittedText는 필수입니다.");
        }

        QuizWrongNote r = new QuizWrongNote();
        r.account = account;
        r.quizQuestion = question;
        r.submittedChoiceId = null;
        r.submittedChoiceText = null;
        r.submittedText = submittedText.trim();
        r.submittedAt = (submittedAt != null ? submittedAt : Instant.now());
        return r;
    }
}
