package com.cygnus.ipoten.quiz_question.entity;

import com.cygnus.ipoten.quiz.entity.QuizSet;
import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "quiz_question",
        indexes = {
                @Index(name = "idx_quiz_question_set", columnList = "quiz_set_id"),
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

    /** 소속 세트(FK) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_set_id", nullable = false)
    private QuizSet quizSet;

    /** 문제 유형 */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    /** 문제 본문 */
    @Setter
    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    /** 문제 전체 해설 */
    @Setter
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    /** 텍스트 정답과 양방향 매핑 */
    @OneToOne(mappedBy = "quizQuestion", fetch = FetchType.LAZY)
    private QuizTextAnswer quizTextAnswer;

    public QuizQuestion(
            Term term,
            TermCategory termCategory,
            QuestionType questionType,
            String questionText,
            QuizSet quizSet,
            String explanation
    ) {
        this.term = term;
        this.termCategory = termCategory;
        this.questionType = questionType;
        this.questionText = questionText;
        this.quizSet = quizSet;
        this.explanation = explanation;
    }

    /** 텍스트 정답형 문제 생성용 (정답 자체는 QuizTextAnswer로 따로) */
    public static QuizQuestion textAnswer(
            Term term,
            TermCategory termCategory,
            QuestionType questionType,
            String questionText,
            QuizSet quizSet,
            String explanation
    ) {
        return new QuizQuestion(term, termCategory, questionType, questionText, quizSet, explanation);
    }

    public QuizQuestion(
            Term term,
            TermCategory termCategory,
            QuestionType questionType,
            String questionText,
            String answerText,
            QuizSet quizSet,
            String explanation
    ) {
        this.term = term;
        this.termCategory = termCategory;
        this.questionType = questionType;
        this.questionText = questionText;
        this.quizSet = quizSet;
        this.explanation = explanation;
    }

    /** AutoQuizGenerator/기본 생성용(answerText, explanation 없이) */
    public QuizQuestion(
            Term term,
            TermCategory termCategory,
            QuestionType questionType,
            String questionText,
            QuizSet quizSet
    ) {
        this(term, termCategory, questionType, questionText, null, quizSet, null);
    }

    public void setQuizSet(QuizSet quizSet) {
        this.quizSet = quizSet;
    }
}
