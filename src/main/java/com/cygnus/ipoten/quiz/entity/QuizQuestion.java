package com.cygnus.ipoten.quiz.entity;

import com.cygnus.ipoten.quiz.entity.enums.QuestionType;
import com.cygnus.ipoten.term.entity.Category;
import com.cygnus.ipoten.term.entity.Term;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "quiz_question",
        indexes = {
                @Index(name = "idx_quiz_question_set_order", columnList = "quiz_set_id, order_index"),
                @Index(name = "idx_quiz_question_category", columnList = "category_id"),
                @Index(name = "idx_quiz_question_term", columnList = "term_id")
        }
)
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 문제의 원천 용어 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = true)
    private Term term;

    /** 카테고리 기반 문제 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    /** 소속 세트(FK) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_set_id", nullable = false)
    private QuizSet quizSet;

    /** 문제 유형 */
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, columnDefinition = "enum('CHOICE', 'OX', 'INITIALS')")
    private QuestionType questionType;

    /** 문제 본문 */
    @Setter
    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    /** 보기형 정답(CHOICE/OX) */
    @Setter
    @Column(name = "answer_index")
    private Integer answerIndex;

    /** 텍스트 정답(초성/주관식/정규식 등) */
    @Setter
    @Column(name="answer_text", length=255)
    private String answerText;

    /** 생성 시 랜덤 로직 사용 여부 */
    @Setter
    @Column(name = "is_random", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean isRandom = false;

    /** 세트 내 정렬용(영속) */
    @Setter
    @Column(name = "order_index")
    private Integer orderIndex;

    /** CHOICE/OX 전용 */
    public QuizQuestion(
            Term term,
            Category category,
            QuestionType questionType,
            String questionText,
            Integer answerIndex
    ) {
        this(term, category, questionType, questionText, answerIndex, null, null, null);
    }

    /** CHOICE/OX 전용 (세트 지정) */
    public QuizQuestion(
            Term term,
            Category category,
            QuestionType questionType,
            String questionText,
            Integer answerIndex,
            QuizSet quizSet
    ) {
        this(term, category, questionType, questionText, answerIndex, null, quizSet, null);
    }

    /** 공용(내부) */
    public QuizQuestion(
            Term term,
            Category category,
            QuestionType questionType,
            String questionText,
            Integer answerIndex,
            String answerText,
            QuizSet quizSet,
            Integer orderIndex
    ) {
        this.term = term;
        this.category = category;
        this.questionType = questionType;
        this.questionText = questionText;
        this.answerIndex = answerIndex;
        this.answerText = answerText;
        this.quizSet = quizSet;
        this.orderIndex = orderIndex;
    }

    /** INITIALS/주관식 전용: 텍스트 정답 */
    public static QuizQuestion textAnswer(
            Term term,
            Category category,
            QuestionType questionType, // 보통 INITIALS 혹은 앞으로 추가될 SHORT_TEXT 등
            String questionText,
            String answerText,
            QuizSet quizSet,
            Integer orderIndex
    ) {
        return new QuizQuestion(term, category, questionType, questionText, null, answerText, quizSet, orderIndex);
    }

    public void setQuizSet(QuizSet quizSet) {
        this.quizSet = quizSet;
    }
}
