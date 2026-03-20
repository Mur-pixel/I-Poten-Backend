package com.cygnus.ipoten.inquiry.entity;

import com.cygnus.ipoten.account.entity.Account;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "inquiries",
        indexes = {
                @Index(name = "idx_inquiries_account_id", columnList = "account_id"),
                @Index(name = "idx_inquiries_status", columnList = "status"),
                @Index(name = "idx_inquiries_type", columnList = "type"),
                @Index(name = "idx_inquiries_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private InquiryType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InquiryStatus status;

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Inquiry(
            Account account,
            InquiryType type,
            String title,
            String content,
            InquiryStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.account = Objects.requireNonNull(account, "account must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.title = normalizeText(title);
        this.content = normalizeText(content);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Inquiry create(
            Account account,
            InquiryType type,
            String title,
            String content
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new Inquiry(
                account,
                type,
                title,
                content,
                InquiryStatus.RECEIVED,
                now,
                now
        );
    }

    public void updateStatus(InquiryStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.updatedAt = LocalDateTime.now();
    }

    public void answer(String answerContent) {
        LocalDateTime now = LocalDateTime.now();

        this.answerContent = normalizeText(answerContent);
        this.status = InquiryStatus.ANSWERED;
        this.answeredAt = now;
        this.updatedAt = now;
    }

    public boolean isOwner(Long accountId) {
        return this.account != null
                && this.account.getId() != null
                && this.account.getId().equals(accountId);
    }

    private static String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}