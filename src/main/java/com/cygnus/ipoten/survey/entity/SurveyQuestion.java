package com.cygnus.ipoten.survey.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 설문 문항 정보를 저장하는 엔티티
 * 문항 제목, 타입, 필수 여부, 순서, 척도 정보를 관리한다
 */
@Getter
@Entity
@Table(
        name = "survey_question",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_survey_question_form_code",
                        columnNames = {"survey_form_id", "question_code"}
                ),
                @UniqueConstraint(
                        name = "uk_survey_question_form_order",
                        columnNames = {"survey_form_id", "display_order"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 문항 PK

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_form_id", nullable = false)
    private SurveyForm surveyForm; // 소속 설문

    @Column(name = "question_code", nullable = false, length = 50)
    private String questionCode; // 문항 코드 예 Q01

    @Column(name = "title", nullable = false, length = 500)
    private String title; // 문항 제목

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private SurveyQuestionType questionType; // 문항 타입

    @Column(name = "is_required", nullable = false)
    private boolean required; // 필수 응답 여부

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder; // 화면 노출 순서

    @Column(name = "scale_min")
    private Integer scaleMin; // 척도형 최소값

    @Column(name = "scale_max")
    private Integer scaleMax; // 척도형 최대값

    @Column(name = "scale_min_label", length = 200)
    private String scaleMinLabel; // 척도형 최소값 설명

    @Column(name = "scale_max_label", length = 200)
    private String scaleMaxLabel; // 척도형 최대값 설명

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // 생성 시각

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // 수정 시각

    public SurveyQuestion(
            SurveyForm surveyForm,
            String questionCode,
            String title,
            SurveyQuestionType questionType,
            boolean required,
            Integer displayOrder,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {
        this.surveyForm = surveyForm;
        this.questionCode = questionCode;
        this.title = title;
        this.questionType = questionType;
        this.required = required;
        this.displayOrder = displayOrder;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.scaleMinLabel = scaleMinLabel;
        this.scaleMaxLabel = scaleMaxLabel;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}