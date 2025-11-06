package com.cygnus.ipoten.quiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quiz_choice",
    uniqueConstraints = @UniqueConstraint(name = "uk_choice_question_order", columnNames = {"quiz_question_id", "sort_order"}),
    indexes = { @Index(name = "idx_choice_question_answer", columnList = "quiz_question_id, is_answer")}
)
public class QuizChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    @Setter
    @Column(name = "choice_text", nullable = false)
    private String choiceText;

    @Setter
    @Column(name = "is_answer", nullable = false)
    private boolean isAnswer;

    @Setter
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String explanation;

    public QuizChoice(QuizQuestion quizQuestion, String choiceText, boolean isAnswer, String explanation) {
        this.quizQuestion = quizQuestion;
        this.choiceText = choiceText;
        this.isAnswer = isAnswer;
        this.explanation = explanation;
    }

}
