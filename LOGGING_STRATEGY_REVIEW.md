# 로깅 전략 도입 시 리소스 비용 · 문제점 · 개선안

> `LOGGING_STRATEGY.md` 기준으로 실제 도입 시 발생하는 부담과 주의사항을 정리한다.

---

## 1. 리소스 비용 증가 예측

### 1-1. DB 쓰기 부하 (INSERT 횟수)

| 시나리오 | 사용자 행동 1회 기준 발생 INSERT 수 |
|----------|----------------------------------|
| 로그인 | `user_session_log` 1건 |
| 회원가입 | `account_lifecycle_log` 2건 (STARTED + COMPLETED) + `user_session_log` 1건 |
| 면접 1회 (5문항 완주) | `interview_event_log` ~8건 (CREATED + PROGRESSED×5 + COMPLETED + CALLBACK_RECEIVED) + `interview_question_log` ~10건 (ASKED×5 + ANSWERED×5) + `credit_attempt_log` 1건 + `notification_delivery_log` 1~2건 = **최대 21건** |
| 회원탈퇴 | `user_session_log` + `account_lifecycle_log` 2건 + `notification_delivery_log` 1건 |

**결론**: 면접 1회 완주 시 단일 사용자 행동으로 약 **20~25 INSERT**가 발생한다.
일 면접 100회 기준 → 하루 **2,000~2,500 INSERT**, 월 **6~7.5만 건** 누적.
초기 규모에선 큰 부담이 아니지만, 일 1,000회 이상이 되면 DB 쓰기 병목 가능성이 생긴다.

---

### 1-2. 스토리지 증가

아래는 테이블별 행 크기 추정이다. (MySQL InnoDB 기준 오버헤드 포함)

| 테이블 | 예상 행 크기 | 월 1만 면접 기준 월 증가량 |
|--------|------------|------------------------|
| `user_session_log` | ~120 byte | ~12 MB/월 (로그인 1만 회) |
| `account_lifecycle_log` | ~150 byte | ~3 MB/월 |
| `interview_event_log` | ~250 byte | ~20 MB/월 (면접당 8행) |
| `interview_question_log` | ~150 byte | ~15 MB/월 (면접당 10행) |
| `notification_delivery_log` | ~200 byte | ~3 MB/월 |
| `credit_attempt_log` | ~100 byte | ~2 MB/월 |
| `review_log` | ~80 byte | ~1 MB/월 |
| **합계** | — | **~56 MB/월** |

6개월이면 약 **330MB**, 인덱스 포함 시 **2배 내외** 추산.
서비스 초기에는 무시 가능하지만 데이터 보존 정책이 없으면 1~2년 후 테이블 풀스캔 쿼리가 눈에 띄게 느려진다.

---

### 1-3. 인덱스 오버헤드

현재 7개 테이블에 정의된 인덱스 총 수: **약 20개**.

- INSERT 1건당 인덱스도 같이 갱신되므로 쓰기 비용이 인덱스 수에 비례해 증가한다.
- `created_at` 단독 인덱스(`idx_usl_created`, `idx_iql_created` 등)는 **카디널리티가 낮아** 풀스캔을 막는 효과가 거의 없다. 단독 인덱스 유지 비용만 추가된다.
- 복합 인덱스 컬럼 순서가 잘못되면 옵티마이저가 사용하지 않는 데드 인덱스가 된다.

---

### 1-4. @Async 스레드 풀 리소스

현재 `LOGGING_STRATEGY.md` 구현 예시는 `@Async` + `REQUIRES_NEW`를 사용하지만 스레드 풀 설정이 명시되어 있지 않다.
Spring Boot 기본 `SimpleAsyncTaskExecutor`는 **요청마다 새 스레드를 생성**하므로, 면접이 동시에 100건 이상 진행되면 스레드 수가 급격히 증가한다.

---

## 2. 구체적인 문제점

### 2-1. `interview_event_log`와 `interview_question_log` 데이터 중복

`interview_event_log.PROGRESSED` (몇 번째 질문까지 진행했는가)와 `interview_question_log.ASKED / ANSWERED` (개별 질문 상태)가 **같은 사실을 두 번 기록**한다.

- 이탈 지점 분석은 `interview_question_log`만으로 충분하다.
- `interview_event_log.PROGRESSED`를 별도로 유지하면 두 테이블 간 데이터 불일치 위험이 생긴다 (한쪽 실패 시).

---

### 2-2. `interview_event_log` 컬럼 희소성(Sparse Column) 문제

`interview_event_log`는 6가지 event 값을 한 테이블에 담는데, **event 타입마다 의미 있는 컬럼이 다르다**.

| event | company | job | career | question_seq | duration_sec | callback_ok |
|-------|---------|-----|--------|--------------|--------------|-------------|
| CREATED | ✅ | ✅ | ✅ | ✗ | ✗ | ✗ |
| PROGRESSED | ✗ | ✗ | ✗ | ✅ | ✗ | ✗ |
| COMPLETED | ✗ | ✗ | ✗ | ✗ | ✅ | ✗ |
| CALLBACK_RECEIVED | ✗ | ✗ | ✗ | ✗ | ✗ | ✅ |
| RESULT_VIEWED | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |

→ 행의 절반 이상이 NULL인 열이 많아 쿼리 시 혼란스럽고, NULL 체크 없이 집계 쿼리를 작성하면 오집계가 발생한다.

---

### 2-3. `account_lifecycle_log.account_id` NULL 허용의 집계 위험

`SIGNUP_STARTED` 시점은 `account_id`가 없으므로 NULL을 허용했다.
그런데 가입 전환율 쿼리에서 `SIGNUP_STARTED`와 `SIGNUP_COMPLETED`를 **같은 테이블에서 날짜 기준으로 카운트**하면,
"특정 날 started한 사람이 다음날 completed" 케이스를 동일 날짜 버킷에 몰아 넣어 전환율이 왜곡된다.
동일 세션을 추적하려면 `session_token` 같은 연결 키가 필요하다.

---

### 2-4. `account_lifecycle_log.meta VARCHAR(255)` 비정형 문제

탈퇴 시 `interview_count` 등을 `meta`에 담기로 했는데, 자유 문자열이면:

- 어드민 쿼리에서 `meta` 값을 파싱해야 해 집계가 복잡하다.
- 저장 형식이 코드마다 달라지면 (예: `"5"` vs `"count:5"`) GROUP BY 시 엉뚱한 결과가 나온다.

---

### 2-5. `credit_attempt_log`에 SUCCESS 케이스 중복 기록

SUCCESS인 크레딧 결제는 이미 `credit_transaction`에 기록된다.
`credit_attempt_log`에도 `result = 'SUCCESS'`를 남기면 **같은 트랜잭션이 두 테이블에 기록**되어 스토리지 낭비 + 나중에 두 테이블 숫자가 맞지 않을 때 혼란이 생긴다.

---

### 2-6. WITHDRAW 이벤트 이중 기록

`user_session_log`와 `account_lifecycle_log` 양쪽에 `WITHDRAW`를 기록하도록 설계되어 있다.
"탈퇴한 사람이 몇 명인가"를 물었을 때 두 테이블 중 어느 것을 기준으로 세야 하는지 정의가 없으면 집계 불일치가 발생한다.

---

### 2-7. ABORTED 이벤트 수집 미완성

`interview_event_log.ABORTED`는 "세션 만료 배치 또는 명시적 end 호출"에서 기록하도록 설계되어 있다.
그런데 **세션 만료 배치가 현재 구현되어 있지 않다면** ABORTED 데이터가 전혀 수집되지 않아 중단율 지표가 0%로 표시된다.

---

### 2-8. `interview_question_log.response_sec` 신뢰성

`response_sec` (질문 수신 ~ 답변 제출 소요 시간)은 서버 입장에서 계산하기 어렵다.
클라이언트가 보내온 타임스탬프를 신뢰해야 하는데, 네트워크 지연, 사용자 기기 시간 오류, 탭 비활성화 등으로 값이 실제와 크게 다를 수 있다.

---

### 2-9. 어드민 집계 쿼리 실시간 부하

현재 설계된 어드민 쿼리는 **모두 실시간 집계 쿼리 (`COUNT`, `SUM`, `AVG`, `GROUP BY`)**이다.
로그 테이블이 수백만 행을 넘으면 어드민 대시보드가 느려지고, 프로덕션 DB에 집계 부하를 그대로 준다.

---

### 2-10. `created_at` 단독 인덱스 효용 부재

`idx_usl_created`, `idx_iql_created`, `idx_ndl_created` 등 `created_at` 단독 인덱스가 여러 테이블에 존재한다.
`WHERE created_at >= X` 쿼리에서 created_at 단독 인덱스는 옵티마이저가 사용할 수도 있지만, 복합 인덱스 `(event, created_at)`이 이미 존재하면 단독 인덱스는 **사실상 데드 인덱스**가 되어 INSERT 비용만 증가한다.

---

## 3. 개선안

### 3-1. 데이터 보존 기간(TTL) 정책 도입

| 테이블 | 권장 보존 기간 | 방법 |
|--------|-------------|------|
| `user_session_log` | 최근 1년 | 월별 파티셔닝 + 오래된 파티션 DROP |
| `interview_event_log` | 최근 1년 | 동일 |
| `interview_question_log` | 최근 6개월 | 상세 로그는 보존 기간 짧게 |
| `notification_delivery_log` | 최근 3개월 | 발송 이슈 SLA 기준 |
| `credit_attempt_log` | 최근 6개월 | 캠페인 분석 주기에 맞춤 |
| `account_lifecycle_log` | 영구 | 계정 감사 목적 |
| `review_log` | 영구 | 서비스 품질 기록 |

**파티셔닝 예시:**
```sql
CREATE TABLE interview_event_log (
    ...
    created_at DATETIME(3) NOT NULL
)
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202601 VALUES LESS THAN (202602),
    PARTITION p202602 VALUES LESS THAN (202603),
    ...
);
```

---

### 3-2. `interview_event_log` PROGRESSED 제거 or 분리

`PROGRESSED` event를 `interview_event_log`에서 제거하고 `interview_question_log`로 일원화한다.
또는 event 유형별로 테이블을 분리한다:

| 테이블 | 담는 이벤트 |
|--------|-----------|
| `interview_session_log` | CREATED / COMPLETED / ABORTED / CALLBACK_RECEIVED / RESULT_VIEWED |
| `interview_question_log` | ASKED / ANSWERED / SKIPPED (이미 존재) |

이렇게 하면 각 테이블의 컬럼이 전부 의미 있게 채워진다.

---

### 3-3. `credit_attempt_log` — INSUFFICIENT만 기록

```sql
-- result = 'SUCCESS' 행 제거, INSUFFICIENT만 기록
INSERT INTO credit_attempt_log (account_id, reason, amount, balance, created_at)
VALUES (?, ?, ?, ?, NOW(3))
WHERE result = 'INSUFFICIENT';  -- 조건부 삽입
```

성공 거래는 `credit_transaction`이 이미 담당한다.
이름도 `credit_insufficient_log`로 변경하면 의도가 명확해진다.

---

### 3-4. `account_lifecycle_log` — session_token 컬럼 추가

가입 전환율을 정확히 추적하려면 `SIGNUP_STARTED`와 `SIGNUP_COMPLETED`를 같은 세션으로 연결할 키가 필요하다.

```sql
ALTER TABLE account_lifecycle_log
    ADD COLUMN session_token VARCHAR(64) NULL COMMENT 'OAuth 세션 연결 키 (STARTED~COMPLETED 추적용)';
```

가입 전환율 쿼리:
```sql
SELECT
    COUNT(DISTINCT s.session_token)                       AS started,
    COUNT(DISTINCT c.session_token)                       AS completed,
    ROUND(COUNT(DISTINCT c.session_token)
          / NULLIF(COUNT(DISTINCT s.session_token), 0) * 100, 1) AS conversion_pct
FROM account_lifecycle_log s
LEFT JOIN account_lifecycle_log c
    ON c.session_token = s.session_token AND c.event = 'SIGNUP_COMPLETED'
WHERE s.event = 'SIGNUP_STARTED'
  AND s.created_at >= NOW() - INTERVAL 30 DAY;
```

---

### 3-5. `meta` 컬럼 → 명시적 컬럼 분리

비정형 `meta VARCHAR(255)` 대신 자주 쓰이는 값을 컬럼으로 분리한다.

```sql
ALTER TABLE account_lifecycle_log
    ADD COLUMN interview_count SMALLINT NULL COMMENT '탈퇴 시점 누적 면접 수',
    ADD COLUMN withdraw_reason VARCHAR(50) NULL COMMENT '탈퇴 사유 선택값';
```

집계가 단순해지고 잘못된 형식 데이터 유입을 막을 수 있다.

---

### 3-6. WITHDRAW 이벤트 단일 테이블로 일원화

`user_session_log`의 WITHDRAW는 제거하고 `account_lifecycle_log`의 WITHDRAW만 유지한다.
`user_session_log`는 세션(로그인/로그아웃) 전용으로 역할을 명확히 한다.

| 테이블 | 담는 이벤트 |
|--------|-----------|
| `user_session_log` | LOGIN / LOGOUT만 |
| `account_lifecycle_log` | SIGNUP_STARTED / SIGNUP_COMPLETED / NICKNAME_UPDATED / WITHDRAW |

---

### 3-7. @Async 전용 스레드 풀 설정 명시

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "eventLogExecutor")
    public Executor eventLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("event-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy()); // 큐 초과 시 로그 DROP (비즈니스 영향 없음)
        executor.initialize();
        return executor;
    }
}

// 사용 시
@Async("eventLogExecutor")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void record(InterviewEventLog eventLog) { ... }
```

기본 `SimpleAsyncTaskExecutor` 대신 풀을 재사용하여 스레드 폭증을 방지한다.

---

### 3-8. 어드민 집계 부하 분리

일별 집계 데이터를 미리 계산해 별도 테이블에 저장하고 어드민은 이 테이블만 조회한다.

```sql
CREATE TABLE daily_stats_cache (
    stat_date   DATE        NOT NULL,
    stat_key    VARCHAR(50) NOT NULL,  -- dau | interview_created | credit_insufficient 등
    stat_value  INT         NOT NULL,
    updated_at  DATETIME(3) NOT NULL,
    PRIMARY KEY (stat_date, stat_key)
);
```

스케줄러 또는 배치가 매일 자정 전날 데이터를 집계해서 INSERT/UPDATE한다.
어드민 대시보드는 프로덕션 DB의 로그 테이블을 직접 GROUP BY하지 않는다.

또는 장기적으로 **OLAP 분리** (Redshift / BigQuery / ClickHouse)를 고려할 수 있다.

---

### 3-9. 데드 인덱스 정리

`created_at` 단독 인덱스는 복합 인덱스가 존재하면 제거한다.

| 제거 대상 | 이유 |
|----------|------|
| `idx_usl_created` | `idx_usl_action_created(action, created_at)` 로 커버됨 |
| `idx_iql_created` | `idx_iql_seq_event(question_seq, event, created_at)` 로 커버됨 |
| `idx_ndl_created` | `idx_ndl_channel_type_created`로 커버됨 |

---

### 3-10. ABORTED 이벤트 — 배치 구현 연계 필수

ABORTED 수집이 의미 있으려면 세션 만료를 탐지하는 배치가 먼저 구현되어야 한다.
그 전까지는 ABORTED를 수집하지 말고 `isFinished = false && created_at < NOW() - INTERVAL 2 HOUR` 쿼리로 임시 대체하는 것이 더 정확하다.

---

## 4. 요약 — 우선순위별 개선 항목

| 우선순위 | 항목 | 이유 |
|---------|------|------|
| 🔴 즉시 | `@Async` 전용 스레드 풀 설정 | 미설정 시 동시 접속 급증으로 스레드 폭증 |
| 🔴 즉시 | `credit_attempt_log` INSUFFICIENT만 기록 | SUCCESS 중복은 데이터 정합성 위험 |
| 🔴 즉시 | WITHDRAW 이벤트 단일 테이블 일원화 | 이중 기록 → 집계 불일치 |
| 🟠 단기 | 데드 인덱스 3개 제거 | 불필요한 쓰기 비용 |
| 🟠 단기 | `account_lifecycle_log` session_token 추가 | 전환율 왜곡 방지 |
| 🟠 단기 | `meta` → 명시적 컬럼 분리 | 집계 정확도 |
| 🟡 중기 | 파티셔닝 + TTL 정책 | 1년 후 쿼리 성능 보호 |
| 🟡 중기 | `interview_event_log` PROGRESSED 제거 | 중복 데이터 구조 정리 |
| 🟡 중기 | `daily_stats_cache` 어드민 집계 분리 | 프로덕션 DB 집계 부하 제거 |
| 🟢 장기 | ABORTED 배치 구현 연계 | 배치 없이 데이터 없음 |
| 🟢 장기 | OLAP 분리 (ClickHouse 등) | 대규모 분석 쿼리 분리 |
