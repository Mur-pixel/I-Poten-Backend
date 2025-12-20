package com.cygnus.ipoten.quiz_set.entity;

import com.cygnus.ipoten.quiz.entity.Quiz;
import com.cygnus.ipoten.job.enums.JobRole;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quiz_set")
public class QuizSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /** 세트의 대표 카테고리 (카테고리 기반 세트일 때 사용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_category_id")
    private TermCategory termCategory;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_set_type", nullable = false)
    private QuizSetType quizSetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_role", length = 64)
    private JobRole jobRole;

    @Column(name = "quiz_set_key", length = 128, unique = true)
    private String quizSetKey;

    public QuizSet(Quiz quiz, String title, QuizSetType quizSetType, TermCategory termCategory) {
        this.quiz = quiz;
        this.title = title;
        this.quizSetType = quizSetType;
        this.termCategory = termCategory;
    }

    public static QuizSet create(Quiz quiz, String title, QuizSetType quizSetType, TermCategory termCategory) {
        return new QuizSet(quiz, title, quizSetType, termCategory);
    }
}
