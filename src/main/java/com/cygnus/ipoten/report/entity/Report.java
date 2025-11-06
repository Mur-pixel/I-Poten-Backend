package com.cygnus.ipoten.report.entity;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고한 생성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AccountProfile reporter;

    // 신고된 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private AccountProfile reportedUser;

    // 신고가 발생한 위치의 타입을 저장
    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    // 신고가 발생한 위치의 id를 저장
    @Column(nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportCategory category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(name = "attachment_s3_key")
    private String attachmentS3Key;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // 생성자
    private Report(AccountProfile reporter, AccountProfile reportedUser, ReportType reportType, Long sourceId, ReportCategory category, String description, String attachmentS3Key) {
        this.reporter = reporter;
        this.reportedUser = reportedUser;
        this.reportType = reportType;
        this.sourceId = sourceId;
        this.category = category;
        this.description = description;
        this.status = ReportStatus.PENDING;
        this.attachmentS3Key = attachmentS3Key; // [추가] 이 줄을 추가해야 합니다.
    }

    // 정적 팩토리 메소드
    public static Report create(AccountProfile reporter, AccountProfile reportedUser, ReportType reportType, Long sourceId, ReportCategory category, String description, String attachmentS3Key) {
        return new Report(reporter, reportedUser, reportType, sourceId, category, description, attachmentS3Key);
    }

    public void updateStatus(ReportStatus status) {
        if (status == null) {
            return;
        }
        this.status = status;
    }
}