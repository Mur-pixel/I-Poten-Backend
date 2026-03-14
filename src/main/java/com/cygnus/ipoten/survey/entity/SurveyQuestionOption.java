package com.cygnus.ipoten.survey.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 객관식 문항의 선택지 정보를 저장하는 엔티티
 */
@Getter
@Entity
@Table(
        name = "survey_question_option",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_survey_question_option_code",
                        columnNames = {"survey_question_id", "option_code"}
                ),
                @UniqueConstraint(
                        name = "uk_survey_question_option_order",
                        columnNames = {"survey_question_id", "display_order"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_question_id", nullable = false)
    private SurveyQuestion surveyQuestion; // 소속 문항

    @Column(name = "option_code", nullable = false, length = 100)
    private String optionCode; // 내부 저장용 코드 예 BACKEND

    @Column(name = "option_label", nullable = false, length = 200)
    private String optionLabel; // 화면 표시용 문구 예 백엔드

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder; // 화면 노출 순서

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // 생성 시각

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // 수정 시각

    public SurveyQuestionOption(
            SurveyQuestion surveyQuestion,
            String optionCode,
            String optionLabel,
            Integer displayOrder
    ) {
        this.surveyQuestion = surveyQuestion;
        this.optionCode = optionCode;
        this.optionLabel = optionLabel;
        this.displayOrder = displayOrder;
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