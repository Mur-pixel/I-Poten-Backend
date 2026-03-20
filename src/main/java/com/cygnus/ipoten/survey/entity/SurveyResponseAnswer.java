package com.cygnus.ipoten.survey.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 설문 문항별 답변 엔티티
 * 객관식은 selectedOptionCode
 * 척도형은 scaleValue
 * 주관식은 textAnswer에 저장
 */
@Getter
@Entity
@Table(
        name = "survey_response_answer",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_survey_response_answer_response_question",
                        columnNames = {"survey_response_id", "survey_question_id"}
                )
        }
)
@NoArgsConstructor
public class SurveyResponseAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_response_id", nullable = false)
    private SurveyResponse surveyResponse; // 소속 응답

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_question_id", nullable = false)
    private SurveyQuestion surveyQuestion; // 대상 문항

    @Column(name = "selected_option_code", length = 100)
    private String selectedOptionCode; // 객관식 선택 코드

    @Column(name = "scale_value")
    private Integer scaleValue; // 척도형 선택 값

    @Column(name = "text_answer", length = 2000)
    private String textAnswer; // 주관식 답변

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // 생성 시각

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // 수정 시각

    public SurveyResponseAnswer(
            SurveyResponse surveyResponse,
            SurveyQuestion surveyQuestion,
            String selectedOptionCode,
            Integer scaleValue,
            String textAnswer
    ) {
        this.surveyResponse = surveyResponse;
        this.surveyQuestion = surveyQuestion;
        this.selectedOptionCode = selectedOptionCode;
        this.scaleValue = scaleValue;
        this.textAnswer = textAnswer;
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