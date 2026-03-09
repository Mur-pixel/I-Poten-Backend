package com.cygnus.ipoten.survey.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 설문 메타 정보를 저장하는 엔티티
 * 설문 제목, 설명, 버전, 활성 여부를 관리한다
 */
@Getter
@Entity
@Table(
        name = "survey_form",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_survey_form_code_version",
                        columnNames = {"code", "version"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 100)
    private String code; // 설문 코드 예 IPOTEN_REVIEW

    @Column(name = "version", nullable = false)
    private Integer version; // 설문 버전

    @Column(name = "title", nullable = false, length = 200)
    private String title; // 설문 제목

    @Column(name = "description", nullable = false, length = 1000)
    private String description; // 설문 설명

    @Column(name = "is_active", nullable = false)
    private boolean active; // 현재 활성 설문 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt; // 생성 시각

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt; // 수정 시각

    public SurveyForm(
            String code,
            Integer version,
            String title,
            String description,
            boolean active
    ) {
        this.code = code;
        this.version = version;
        this.title = title;
        this.description = description;
        this.active = active;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
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