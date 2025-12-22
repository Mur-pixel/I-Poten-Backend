package com.cygnus.ipoten.quiz_session.entity;

import com.cygnus.ipoten.quiz_session.entity.enums.SessionSourceType;
import com.cygnus.ipoten.quiz_set.entity.enums.QuizSetType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.quiz_session.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionMode;
import com.cygnus.ipoten.quiz_session.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * QuizSession
 *
 * 사용자가 특정 퀴즈 세트를 푸는 한 번의 세션을 나타내는 엔티티.
 * 1) 진행/제출 상태와 점수(세션 라이프사이클)
 * 2) 실제로 출제된 문제 ID 스냅샷(재현/리뷰/페이징)
 * 3) 세션을 생성한 출처/필터 메타(타임라인/검색/분석용)
 */
@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "quiz_session",
        indexes = {
                @Index(name = "idx_qs_started", columnList = "started_at"),
                @Index(name = "idx_qs_user_status_started", columnList = "account_id, session_status, started_at"),
                @Index(name = "idx_qs_user_sourcekey_started", columnList = "account_id, source_key, started_at"),
                @Index(name = "idx_qs_user_source_started", columnList = "account_id, source_type, source_id, started_at")
        }
)
public class QuizSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // 세션 ID

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;    // 응시 사용자

    /** WRONG_ONLY일 때 원본(전체) 세션을 가리킴 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_session_id")
    private QuizSession parentSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_mode", nullable = false, length = 20)
    private SessionMode sessionMode;    // 세션 모드(FULL, WRONG_ONLY)

    @Enumerated(EnumType.STRING)
    @Column(name = "session_status", nullable = false, length = 20)
    private SessionStatus sessionStatus;    // 세션 진행 상태(IN_PROGRESS, SUBMITTED, EXPIRED)

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;    // 해당 세트 기준 몇 번째 응시인지 (1, 2, 3…)

    /** WRONG_ONLY 같은 동적 세트의 문제 스냅샷(JSON: [qId1, qId2, ...]) */
    @Column(name = "questions_snapshot_json", columnDefinition = "json")
    private String questionsSnapshotJson;

    @Column(name = "score")
    private Integer score; // 맞힌 개수(분모는 total 또는 snapshot 길이)

    @Column(name = "total")
    private Integer total; // 총 문항 수

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "elapsed_ms")
    private Long elapsedMs; // 제출까지 걸린 시간(ms), null 허용

    @Enumerated(EnumType.STRING)
    @Column(name = "seed_mode", length = 20)
    private SeedMode seedMode;  // AUTO | DAILY | FIXED

    @Column(name ="seed_value")
    private Long seedValue; // 최종 해석된 시드 값

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SessionSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_key", nullable = false, length = 200)
    private String sourceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "part_type", length = 20)
    private QuizSetType partType;

    // 마지막 활동 시각(조회/답안 저장/제출 시 갱신)
    private Instant lastActivityAt;

    public void submit(int finalScore) {
        this.sessionStatus = SessionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.score = finalScore;
    }

    public void submit(int finalScore, Long elapsedMs) {
        if (this.sessionStatus == SessionStatus.SUBMITTED || this.sessionStatus == SessionStatus.EXPIRED) {
            throw new IllegalStateException("이미 제출되었거나 만료된 세션입니다. id=" + id);
        }
        this.sessionStatus = SessionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.score = finalScore;
        this.elapsedMs = elapsedMs;
    }

    public void expire() {
        if (this.sessionStatus == SessionStatus.SUBMITTED || this.sessionStatus == SessionStatus.EXPIRED) {
            throw new IllegalStateException("이미 제출되었거나 만료된 세션입니다. id=" + id);
        }
        this.sessionStatus = SessionStatus.EXPIRED;
    }

    public void begin(Account account,
                      SessionMode sessionMode, int attemptNo,
                      int total, String questionsSnapshotJson) {
        this.account = account;
        this.sessionMode = sessionMode;
        this.sessionStatus = SessionStatus.IN_PROGRESS;
        this.attemptNo = attemptNo;
        this.startedAt = Instant.now();
        this.total = total;
        this.questionsSnapshotJson = questionsSnapshotJson;
        this.lastActivityAt = Instant.now();
    }

    public void begin(Account account,
                      SessionMode sessionMode, int attemptNo,
                      int total, String questionsSnapshotJson,
                      SeedMode seedMode, Long seedValue) {
        this.account = account;
        this.sessionMode = sessionMode;
        this.sessionStatus = SessionStatus.IN_PROGRESS;
        this.attemptNo = attemptNo;
        this.startedAt = Instant.now();
        this.total = total;
        this.questionsSnapshotJson = questionsSnapshotJson;
        this.seedMode = seedMode;
        this.seedValue = seedValue;
        this.lastActivityAt = Instant.now();
    }

    public void beginFromSourceWithParent(
            Account account,
            QuizSession parent,
            SessionSourceType sourceType,
            Long sourceId,
            String sourceKey,
            QuizSetType partType,
            SessionMode sessionMode,
            int attemptNo,
            int total,
            String snapshotJson,
            SeedMode seedMode,
            Long seedValue
    ) {
        if (account == null) throw new IllegalArgumentException("account required");
        if (parent == null) throw new IllegalArgumentException("parentSession required");
        if (sourceType == null) throw new IllegalArgumentException("sourceType required");
        if (sourceId == null) throw new IllegalArgumentException("sourceId required");
        if (sourceKey == null || sourceKey.isBlank()) throw new IllegalArgumentException("sourceKey required");
        if (sessionMode == null) throw new IllegalArgumentException("sessionMode required");
        if (attemptNo <= 0) throw new IllegalArgumentException("attemptNo must be >= 1");
        if (total <= 0) throw new IllegalArgumentException("total must be >= 1");
        if (snapshotJson == null || snapshotJson.isBlank()) throw new IllegalArgumentException("snapshotJson required");
        if (seedMode == null) throw new IllegalArgumentException("seedMode required");
        if (seedValue == null) throw new IllegalArgumentException("seedValue required");

        this.account = account;
        this.parentSession = parent;

        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceKey = sourceKey;
        this.partType = partType;

        this.sessionMode = sessionMode;
        this.sessionStatus = SessionStatus.IN_PROGRESS;
        this.attemptNo = attemptNo;
        this.startedAt = Instant.now();

        this.total = total;
        this.questionsSnapshotJson = snapshotJson;

        this.seedMode = seedMode;
        this.seedValue = seedValue;

        this.lastActivityAt = Instant.now();
    }

    public void beginFromSource(
            Account account,
            SessionSourceType sourceType,
            Long sourceId,
            String sourceKey,
            QuizSetType partType,
            SessionMode sessionMode,
            int attemptNo,
            int total,
            String snapshotJson,
            SeedMode seedMode,
            Long seedValue
    ) {
        this.account = account;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceKey = sourceKey;
        this.partType = partType;

        this.sessionMode = sessionMode;
        this.sessionStatus = SessionStatus.IN_PROGRESS;
        this.attemptNo = attemptNo;
        this.startedAt = Instant.now();
        this.total = total;
        this.questionsSnapshotJson = snapshotJson;

        this.seedMode = seedMode;
        this.seedValue = seedValue;

        this.lastActivityAt = Instant.now();
    }

    public List<Long> getSnapshotQuestionIds() {
        try {
            if (questionsSnapshotJson == null || questionsSnapshotJson.isBlank()) return List.of();
            return new ObjectMapper().readValue(questionsSnapshotJson, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("유효하지 않은 질문 snapshot json: " + questionsSnapshotJson, e);
        }
    }

    public void touchActivity() {
        this.lastActivityAt = Instant.now();
    }
}
