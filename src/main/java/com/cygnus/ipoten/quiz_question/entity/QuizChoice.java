package com.cygnus.ipoten.quiz_question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "quiz_choice",
        indexes = {
                @Index(name = "idx_choice_question_answer", columnList = "quiz_question_id, is_answer")
        }
)
public class QuizChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 퀴즈 문제(QuizQuestion)에 속한 보기인지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_question_id", nullable = false)
    private QuizQuestion quizQuestion;

    /** 보기 텍스트 */
    @Column(name = "choice_text", nullable = false)
    private String choiceText;

    /** 정답 여부 */
    @Column(name = "is_answer", nullable = false)
    private boolean isAnswer;

    public QuizChoice(QuizQuestion quizQuestion, String choiceText, boolean isAnswer) {
        this.quizQuestion = quizQuestion;
        this.choiceText = choiceText;
        this.isAnswer = isAnswer;
    }

    public static QuizChoice create(QuizQuestion quizQuestion, String choiceText, boolean isAnswer) {
        if (quizQuestion == null) {
            throw new IllegalArgumentException("quizQuestion은 필수입니다.");
        }
        if (choiceText == null || choiceText.isBlank()) {
            throw new IllegalArgumentException("choiceText는 필수입니다.");
        }
        return new QuizChoice(quizQuestion, choiceText, isAnswer);
    }
}
