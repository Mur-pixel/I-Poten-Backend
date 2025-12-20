package com.cygnus.ipoten.quiz_review.entity;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.quiz_question.entity.QuizChoice;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_choice_id", nullable = false)
    private QuizChoice quizChoice; // 선택한 보기(보통 오답). 텍스트 답안이면 null 여지도 고려

    private Instant submittedAt; // 오답 저장 시각
    private String explanation; // 해설

    public static QuizWrongNote create(Account account,
                                       QuizQuestion question,
                                       QuizChoice choice,
                                       String explanation) {
        return create(account, question, choice, explanation, Instant.now());
    }

    public static QuizWrongNote create(Account account,
                                       QuizQuestion question,
                                       QuizChoice choice,
                                       String explanation,
                                       Instant submittedAt) {
        QuizWrongNote r = new QuizWrongNote();
        r.account = account;
        r.quizQuestion = question;
        r.quizChoice = choice;
        r.explanation = explanation;
        r.submittedAt = (submittedAt != null ? submittedAt : Instant.now());
        return r;
    }
}
