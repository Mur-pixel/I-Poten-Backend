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
 * - QuizSession 과 다대일 관계 (세션 단위 응답 집합)
 * - QuizQuestion 과 다대일 관계 (어떤 문제에 대한 응답인지)
 * - QuizChoice 와 다대일 관계 (사용자가 고른 보기)
 * - 응답 시각(submittedAt)과 정답 여부(isCorrect)를 함께 기록
 *
 * 제약 조건:
 * - 동일 세션 내에서 같은 문제에 대한 응답은 하나만 존재하도록 UniqueConstraint(session_id, quiz_question_id)
 * - 세션별/문제별 조회를 빠르게 하기 위해 인덱스 추가
 *
 * 활용 예:
 * - 채점 시 정답 여부 저장
 * - 세션 리뷰 화면에서 사용자가 어떤 보기를 선택했는지 조회
 * - 학습 통계(정답률, 오답노트) 집계의 근거 데이터
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_choice_id")
    private QuizChoice quizChoice;  // 선택한 보기 ID

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;  // 응답 시각

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;  // 정답 여부

    @Column(name = "submitted_text", length = 255)
    private String submittedText; // INITIALS/주관식 제출값

    public QuizSessionAnswer(QuizSession quizSession, QuizQuestion quizQuestion, QuizChoice quizChoice, Instant submittedAt, boolean isCorrect) {
        this.quizSession = quizSession;
        this.quizQuestion = quizQuestion;
        this.quizChoice = quizChoice;
        this.submittedAt = submittedAt;
        this.isCorrect = isCorrect;
    }
}