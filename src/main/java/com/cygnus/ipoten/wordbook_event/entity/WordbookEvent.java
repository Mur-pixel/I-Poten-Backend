package com.cygnus.ipoten.wordbook_event.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "wordbook_event",
        indexes = {
                @Index(name = "idx_wordbook_event_created_at", columnList = "created_at"),
                @Index(name = "idx_wordbook_event_account_day", columnList = "account_id, created_at"),
                @Index(name = "idx_wordbook_event_account_type_day", columnList = "account_id, event_type, created_at"),
                @Index(name = "idx_wordbook_event_term_day", columnList = "term_id, created_at")
        })
public class WordbookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name ="event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "wordbook_id")
    private Long wordbookId;

    @Column(name = "term_id")
    private Long termId;

    // MEMO_STATUS_CHANGED일 때만 의미 있음 (DONE / LEARNING 등)
    @Column(name = "memo_status", length = 20)
    private String memoStatus;

    // bulk 저장 개수 / pdf 용어 개수 같은 숫자
    @Column(name = "amount")
    private Integer amount;

    // pdfId 같은 부가 값
    @Column(name = "extra", length = 100)
    private String extra;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    private WordbookEvent(Long accountId, String eventType, Long wordbookId, Long termId, String memoStatus, Integer amount, String extra) {
        this.createdAt = Instant.now();
        this.accountId = accountId;
        this.eventType = eventType;
        this.wordbookId = wordbookId;
        this.termId = termId;
        this.memoStatus = memoStatus;
        this.amount = amount;
        this.extra = extra;
    }

    public static WordbookEvent create(Long accountId, String eventType, Long wordbookId, Long termId, String memoStatus, Integer amount, String extra) {
        return new WordbookEvent(accountId, eventType, wordbookId, termId, memoStatus, amount, extra);
    }
}
