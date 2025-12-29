package com.cygnus.ipoten.quiz_set.entity;

import com.cygnus.ipoten.quiz_question.entity.QuizQuestion;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "quiz_set_question",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_qsq_set_question",
                        columnNames = {"quiz_set_id", "quiz_question_id"}
                )
        },
        indexes = {
                @Index(name = "idx_qsq_set", columnList = "quiz_set_id"),
                @Index(name = "idx_qsq_question", columnList = "quiz_question_id")
        }
)
public class QuizSetQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 세트에 속하는지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_set_id", nullable = false)
    private QuizSet quizSet;

    /** 어떤 문제인지 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    private QuizSetQuestion(QuizSet quizSet, QuizQuestion quizQuestion) {
        if (quizSet == null) throw new IllegalArgumentException("quizSet은 필수입니다.");
        if (quizQuestion == null) throw new IllegalArgumentException("quizQuestion은 필수입니다.");
        this.quizSet = quizSet;
        this.quizQuestion = quizQuestion;
    }

    public static QuizSetQuestion create(QuizSet quizSet, QuizQuestion quizQuestion) {
        return new QuizSetQuestion(quizSet, quizQuestion);
    }

}
