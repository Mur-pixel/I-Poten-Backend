package com.cygnus.ipoten.quiz.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.quiz.entity.enums.SeedMode;
import com.cygnus.ipoten.quiz.entity.enums.SessionMode;
import com.cygnus.ipoten.quiz.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * UserQuizSession
 *
 * 사용자가 특정 퀴즈 세트를 푸는 한 번의 세션을 나타내는 엔티티.
 * - 사용자 × 퀴즈 세트 응시 이력 저장
 * - 세트 단위로 진행 상태(IN_PROGRESS, SUBMITTED, EXPIRED)와 점수를 기록한다.
 * - FULL 모드 또는 WRONG_ONLY 모드로 세션을 시작할 수 있다.
 * - parentSession을 통해 WRONG_ONLY 세션은 원본 세션과 연결된다.
 * - questionsSnapshotJson에 실제 출제된 문제 ID 스냅샷을 저장한다.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "user_quiz_session",
    indexes = {
            @Index(name = "idx_uqs_user_set", columnList = "account_id, quiz_set_id, started_at"),
            @Index(name = "idx_uqs_started", columnList = "started_at")
    }
)
public class UserQuizSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // 세션 ID

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;    // 응시 사용자

    @Setter(AccessLevel.PACKAGE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_set_id", nullable = false)
    private QuizSet quizSet;    // 푼 퀴즈 세트

    /** WRONG_ONLY일 때 원본(전체) 세션을 가리킴 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_session_id")
    private UserQuizSession parentSession;

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
    private Long seed; // 최종 해석된 시드 값

    // 마지막 활동 시각(조회/답안 저장/제출 시 갱신)
    private Instant lastActivityAt;

    public void submit(int finalScore) {
        this.sessionStatus = SessionStatus.SUBMITTED;
        this.submittedAt = Instant.now();
        this.score = finalScore;
    }

    public void submit(int finalScore, Long elapsedMs) {
        submit(finalScore);
        this.elapsedMs = elapsedMs;
    }

    public void expire() {
        this.sessionStatus = SessionStatus.EXPIRED;
    }

    public void begin(Account account, QuizSet quizSet,
                      SessionMode sessionMode, int attemptNo,
                      int total, String questionsSnapshotJson) {
        this.account = account;
        this.quizSet = quizSet;
        this.sessionMode = sessionMode;
        this.sessionStatus = SessionStatus.IN_PROGRESS;
        this.attemptNo = attemptNo;
        this.startedAt = Instant.now();
        this.total = total;
        this.questionsSnapshotJson = questionsSnapshotJson;
        this.lastActivityAt = Instant.now();
    }

    public void begin(Account account, QuizSet quizSet,
                      SessionMode sessionMode, int attemptNo,
                      int total, String questionsSnapshotJson,
                      SeedMode seedMode, Long seed) {
        this.account = account;
        this.quizSet = quizSet;
        this.sessionMode = sessionMode;
        this.sessionStatus = SessionStatus.IN_PROGRESS;
        this.attemptNo = attemptNo;
        this.startedAt = Instant.now();
        this.total = total;
        this.questionsSnapshotJson = questionsSnapshotJson;
        this.seedMode = seedMode;
        this.seed = seed;
        this.lastActivityAt = Instant.now();
    }

    public void beginWithParent(Account account, QuizSet quizSet, UserQuizSession parent,
                                SessionMode mode, Integer attemptNo, Integer total, String snapshotJson) {
        this.account = account;
        this.quizSet = quizSet;
        this.parentSession = parent;
        this.sessionMode = mode;
        this.sessionStatus = SessionStatus.IN_PROGRESS;
        this.attemptNo = attemptNo;
        this.startedAt = Instant.now();
        this.total = total;
        this.questionsSnapshotJson = snapshotJson;
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
