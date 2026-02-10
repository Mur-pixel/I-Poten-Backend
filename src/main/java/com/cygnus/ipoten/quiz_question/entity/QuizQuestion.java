package com.cygnus.ipoten.quiz_question.entity;

import com.cygnus.ipoten.quiz_question.entity.enums.DifficultyLevel;
import com.cygnus.ipoten.quiz_question.entity.enums.QuestionType;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "quiz_question",
        indexes = {
                @Index(name = "idx_quiz_question_category", columnList = "term_category_id"),
                @Index(name = "idx_quiz_question_term", columnList = "term_id")
        }
)
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문제의 원천 용어 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    /** 카테고리 기반 문제 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_category_id")
    private TermCategory termCategory;

    /** 문제 유형 */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    /** 난이도 */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private DifficultyLevel difficulty;

    /** 문제 본문 */
    @Setter
    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    /** 문제 전체 해설 */
    @Setter
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    /** 질문 생성 시간 */
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    /** 질문 업데이트 시간 */
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

    /** 텍스트 정답과 양방향 매핑 */
    @OneToOne(mappedBy = "quizQuestion", fetch = FetchType.LAZY)
    private QuizTextAnswer quizTextAnswer;

    public QuizQuestion(
            Term term,
            TermCategory termCategory,
            QuestionType questionType,
            DifficultyLevel difficulty,
            String questionText,
            String explanation
    ) {
        this.term = term;
        this.termCategory = termCategory;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.explanation = explanation;
    }

    public QuizQuestion(
            Term term,
            TermCategory termCategory,
            QuestionType questionType,
            DifficultyLevel difficulty,
            String questionText,
            String answerText,
            String explanation
    ) {
        this.term = term;
        this.termCategory = termCategory;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.explanation = explanation;

        if (answerText != null && !answerText.isBlank()) {
            this.upsertTextAnswer(answerText);
        }
    }

    /** AutoQuizGenerator/기본 생성용(answerText, explanation 없이) */
    public QuizQuestion(
            Term term,
            TermCategory termCategory,
            QuestionType questionType,
            DifficultyLevel difficulty,
            String questionText
    ) {
        this(term, termCategory, questionType, difficulty, questionText, null);
    }

    public QuizTextAnswer upsertTextAnswer(String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("텍스트 정답은 필수입니다.");
        }

        if (this.quizTextAnswer == null) {
            QuizTextAnswer created = QuizTextAnswer.create(this, answerText);
            this.quizTextAnswer = created;
            return created;
        }

        this.quizTextAnswer.changeAnswerText(answerText);
        return this.quizTextAnswer;
    }

    public void detachTextAnswer() {
        this.quizTextAnswer = null;
    }
}
