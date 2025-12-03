package com.cygnus.ipoten.quiz_question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_text_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizTextAnswer {

    @Id
    @Column(name = "quiz_question_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "quiz_question_id")
    private QuizQuestion quizQuestion;

    @Column(name = "answer_text", nullable = false, length = 255)
    private String answerText;

    private QuizTextAnswer(QuizQuestion question, String answerText) {
        this.quizQuestion = question;
        this.answerText = answerText;
    }

    public static QuizTextAnswer create(QuizQuestion question, String answerText) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("텍스트 정답은 필수입니다.");
        }
        return new QuizTextAnswer(question, answerText);
    }
}