package com.cygnus.ipoten.survey.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 설문 응답 헤더 엔티티
 * 누가 어떤 설문에 언제 제출했는지 저장
 */
@Getter
@Entity
@Table(
        name = "survey_response",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_survey_response_form_account",
                        columnNames = {"survey_form_id", "account_id"}
                )
        }
)
@NoArgsConstructor
public class SurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_form_id", nullable = false)
    private SurveyForm surveyForm; // 응답 대상 설문

    @Column(name = "account_id", nullable = false)
    private Long accountId; // 제출한 사용자 계정 ID

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt; // 제출 시각

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // 생성 시각

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // 수정 시각

    public SurveyResponse(
            SurveyForm surveyForm,
            Long accountId,
            Instant submittedAt
    ) {
        this.surveyForm = surveyForm;
        this.accountId = accountId;
        this.submittedAt = submittedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.submittedAt == null) {
            this.submittedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
