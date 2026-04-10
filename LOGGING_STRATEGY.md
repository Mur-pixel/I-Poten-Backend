# I-Poten 로깅 전략

> 퀴즈·용어·노트 도메인 제외.
> **어드민 가시화 + 비즈니스 의사결정**에 실제로 쓰이는 데이터만 설계한다.

---

## 0. 기존에 이미 존재하는 로그

새로 만들기 전에 현재 테이블로 이미 답할 수 있는 질문을 확인한다.

| 테이블 | 커버하는 질문 |
|--------|-------------|
| `interview` | 회사별·유형별 면접 생성 수, 플랜 분포, isFinished 완료 여부 |
| `credit_transaction` | 크레딧 충전·사용·환불 추이 (reason 표준화 필요 → 섹션 8 참고) |
| `account` | 소셜 로그인 provider별 신규 가입자 추이 |

**이것만으로 답할 수 없는 질문**이 아래 신규 테이블의 설계 근거다.

---

## 1. `user_session_log` — 세션 이벤트

### 답할 수 있는 질문
- DAU / WAU / MAU 는 몇 명인가?
- provider별 실제 활성 사용자 비율은?
- 웹 vs 모바일 사용 비율은?
- 재방문율 (리텐션) 은?
- 회원탈퇴 직전 마지막 로그인은 언제인가?

### DDL
```sql
CREATE TABLE user_session_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT      NOT NULL,
    action      VARCHAR(20) NOT NULL,  -- LOGIN | LOGOUT | WITHDRAW
    provider    VARCHAR(20),           -- KAKAO | GOOGLE | NAVER | GITHUB | APPLE | META
    platform    VARCHAR(10),           -- WEB | MOBILE
    created_at  DATETIME(3) NOT NULL,

    INDEX idx_usl_account_created (account_id, created_at),
    INDEX idx_usl_action_created  (action, created_at),
    INDEX idx_usl_created         (created_at)
);
```

### 기록 시점
| action | 기록 위치 |
|--------|----------|
| `LOGIN` | 각 OAuth 서비스 로그인 완료 직후 |
| `LOGOUT` | `AuthenticationServiceImpl.logout()` 완료 후 |
| `WITHDRAW` | `AccountService.withdraw()` 완료 후 |

### 어드민 활용 쿼리
```sql
-- DAU
SELECT DATE(created_at) AS day, COUNT(DISTINCT account_id) AS dau
FROM user_session_log
WHERE action = 'LOGIN'
GROUP BY day ORDER BY day DESC LIMIT 30;

-- provider별 활성 사용자 (최근 30일)
SELECT provider, COUNT(DISTINCT account_id) AS active_users
FROM user_session_log
WHERE action = 'LOGIN' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY provider ORDER BY active_users DESC;

-- 웹 vs 모바일 비율
SELECT platform, COUNT(DISTINCT account_id) AS users
FROM user_session_log
WHERE action = 'LOGIN' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY platform;
```

---

## 2. `account_lifecycle_log` — 계정 생명주기

### 기존 `account` 테이블의 한계
- 가입 시점만 기록됨
- 회원탈퇴 사유, 탈퇴 전 마지막 활동, 면접 이용 여부를 연결할 수 없음
- 닉네임 변경 이력 없음

### 답할 수 있는 질문
- 가입 → 첫 면접까지 며칠 걸리는가? (활성화 전환율)
- OAuth 임시 토큰까지 생성했지만 가입을 완료하지 않은 이탈 수는?
- 탈퇴 유저의 평균 면접 횟수는? (이탈 원인 단서)
- 닉네임 변경 빈도는?

### DDL
```sql
CREATE TABLE account_lifecycle_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT,                -- 가입 전 이탈이면 NULL 가능
    event       VARCHAR(30)  NOT NULL, -- SIGNUP_STARTED | SIGNUP_COMPLETED | NICKNAME_UPDATED | WITHDRAW
    provider    VARCHAR(20),           -- SIGNUP_STARTED / COMPLETED 시점에만
    platform    VARCHAR(10),           -- WEB | MOBILE
    meta        VARCHAR(255),          -- 추가 정보 (예: 탈퇴 시 interview_count)
    created_at  DATETIME(3)  NOT NULL,

    INDEX idx_all_event_created   (event, created_at),
    INDEX idx_all_account_created (account_id, created_at)
);
```

### Event 값 정의
| event | 설명 | 기록 위치 |
|-------|------|----------|
| `SIGNUP_STARTED` | OAuth 완료, 임시 토큰 발급 (회원가입 화면 진입) | 각 OAuth 서비스 신규 유저 분기 |
| `SIGNUP_COMPLETED` | `/api/account/signup` 완료 | `SignupServiceImpl.signup()` |
| `NICKNAME_UPDATED` | 닉네임 변경 성공 | `AccountProfileServiceImp.updateNickname()` |
| `WITHDRAW` | 회원탈퇴 | `AccountService.withdraw()` |

### 어드민 활용 쿼리
```sql
-- 가입 전환율 (STARTED → COMPLETED)
SELECT
    DATE(created_at)                            AS day,
    SUM(event = 'SIGNUP_STARTED')               AS started,
    SUM(event = 'SIGNUP_COMPLETED')             AS completed,
    ROUND(SUM(event = 'SIGNUP_COMPLETED')
          / NULLIF(SUM(event = 'SIGNUP_STARTED'), 0) * 100, 1) AS conversion_pct
FROM account_lifecycle_log
WHERE event IN ('SIGNUP_STARTED', 'SIGNUP_COMPLETED')
  AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY day ORDER BY day DESC;

-- 탈퇴 유저 meta (interview_count 등) 분석
SELECT meta, COUNT(*) AS cnt
FROM account_lifecycle_log
WHERE event = 'WITHDRAW'
  AND created_at >= NOW() - INTERVAL 90 DAY
GROUP BY meta ORDER BY cnt DESC;
```

---

## 3. `interview_event_log` — 면접 세션 이벤트

### 기존 `interview` 테이블의 한계
- 생성 시점만 기록, 완료/중단 시점·소요 시간 없음
- 플랜(PREMIUM/NORMAL)별 완료율, 직군별 비교 불가
- FastAPI 콜백 수신 여부 추적 불가

### 답할 수 있는 질문
- 회사별 면접 생성 수 Top N은?
- 면접 유형(TECHNICAL / COMPANY / PERSONAL)별 완료율은?
- 플랜(PREMIUM / NORMAL)별 사용 분포는?
- 면접 1회 평균 소요 시간은?
- FastAPI 콜백 실패율은?
- 직군(job)별 면접 수는?
- 경력별 면접 비율은?

### DDL
```sql
CREATE TABLE interview_event_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT       NOT NULL,
    interview_id    BIGINT       NOT NULL,
    event           VARCHAR(30)  NOT NULL,  -- CREATED | PROGRESSED | COMPLETED | ABORTED | CALLBACK_RECEIVED | RESULT_VIEWED
    interview_type  VARCHAR(30),            -- TECHNICAL | COMPANY | PERSONAL
    plan            VARCHAR(20),            -- PREMIUM | NORMAL
    company         VARCHAR(100),           -- sender 값
    job             VARCHAR(50),            -- BACKEND | FRONTEND | AI | DEVOPS 등
    career          VARCHAR(30),            -- 신입 | 3년 이하 | 5년 이하 ...
    question_seq    TINYINT,               -- PROGRESSED 시점: 몇 번째 질문 (1~5)
    duration_sec    INT,                    -- COMPLETED 시점: 생성~완료 소요 초
    callback_ok     BOOLEAN,               -- CALLBACK_RECEIVED 시점: FastAPI 응답 정상 여부
    created_at      DATETIME(3)  NOT NULL,

    INDEX idx_iel_account_created     (account_id, created_at),
    INDEX idx_iel_event_created       (event, created_at),
    INDEX idx_iel_company_event       (company, event, created_at),
    INDEX idx_iel_type_event_created  (interview_type, event, created_at),
    INDEX idx_iel_interview_id        (interview_id)
);
```

### Event 값 정의
| event | 설명 | 기록 위치 |
|-------|------|----------|
| `CREATED` | 면접 세션 생성 완료 | `InterviewServiceImpl.createInterview()` |
| `PROGRESSED` | 지원자가 답변 제출, 다음 질문 수신 | `InterviewServiceImpl.execute()` |
| `COMPLETED` | 면접 정상 종료 | `InterviewServiceImpl.endInterview()` |
| `ABORTED` | 면접 중도 이탈 | 세션 만료 배치 or 명시적 end 호출 |
| `CALLBACK_RECEIVED` | FastAPI 평가 결과 콜백 수신 | `InterviewController.callback()` |
| `RESULT_VIEWED` | 유저가 결과 조회 | `InterviewController.getInterviewResult()` |

### 어드민 활용 쿼리
```sql
-- 회사별 면접 생성 Top 20 (최근 30일)
SELECT company, COUNT(*) AS created_count
FROM interview_event_log
WHERE event = 'CREATED' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY company ORDER BY created_count DESC LIMIT 20;

-- 유형별 완료율
SELECT
    interview_type,
    SUM(event = 'CREATED')   AS created,
    SUM(event = 'COMPLETED') AS completed,
    ROUND(SUM(event = 'COMPLETED') / NULLIF(SUM(event = 'CREATED'),0) * 100, 1) AS completion_pct
FROM interview_event_log
WHERE created_at >= NOW() - INTERVAL 30 DAY
GROUP BY interview_type;

-- 직군별 면접 수
SELECT job, COUNT(*) AS cnt
FROM interview_event_log
WHERE event = 'CREATED' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY job ORDER BY cnt DESC;

-- FastAPI 콜백 실패율
SELECT
    DATE(created_at) AS day,
    SUM(callback_ok = FALSE)                             AS failed,
    COUNT(*)                                              AS total,
    ROUND(SUM(callback_ok = FALSE) / COUNT(*) * 100, 1) AS fail_pct
FROM interview_event_log
WHERE event = 'CALLBACK_RECEIVED'
  AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY day ORDER BY day DESC;

-- 평균 면접 소요 시간 (직군별)
SELECT job, ROUND(AVG(duration_sec) / 60, 1) AS avg_min
FROM interview_event_log
WHERE event = 'COMPLETED' AND duration_sec IS NOT NULL
  AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY job ORDER BY avg_min DESC;
```

---

## 4. `interview_question_log` — 질문별 이탈 분석

### 기존 로그의 한계
`interview_event_log`의 `PROGRESSED`만으로는 몇 번째 질문에서 이탈했는지 정밀하게 파악하기 어렵다.

### 답할 수 있는 질문
- 몇 번째 질문에서 가장 많이 이탈하는가?
- 기술 꼬리 질문(5번)까지 도달하는 비율은?
- 질문 유형(인성/프로젝트/기술)별 답변 소요 시간은?
- 답변 길이(글자 수) 평균은?

### DDL
```sql
CREATE TABLE interview_question_log (
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    interview_id    BIGINT      NOT NULL,
    account_id      BIGINT      NOT NULL,
    question_seq    TINYINT     NOT NULL,  -- 1~5
    question_type   VARCHAR(20),           -- INTRO | PERSONALITY | PROJECT | PROJECT_FOLLOWUP | TECH
    event           VARCHAR(20) NOT NULL,  -- ASKED | ANSWERED | SKIPPED
    answer_length   INT,                   -- ANSWERED 시점: 답변 글자 수
    response_sec    INT,                   -- ANSWERED 시점: 질문 수신 ~ 제출 소요 초
    created_at      DATETIME(3) NOT NULL,

    INDEX idx_iql_interview  (interview_id),
    INDEX idx_iql_seq_event  (question_seq, event, created_at),
    INDEX idx_iql_created    (created_at)
);
```

### 어드민 활용 쿼리
```sql
-- 질문 번호별 이탈률
SELECT
    question_seq,
    SUM(event = 'ASKED')    AS asked,
    SUM(event = 'ANSWERED') AS answered,
    ROUND(SUM(event = 'ANSWERED') / NULLIF(SUM(event = 'ASKED'),0) * 100, 1) AS answer_rate
FROM interview_question_log
WHERE created_at >= NOW() - INTERVAL 30 DAY
GROUP BY question_seq ORDER BY question_seq;

-- 질문 유형별 평균 답변 소요 시간
SELECT question_type, ROUND(AVG(response_sec), 1) AS avg_sec
FROM interview_question_log
WHERE event = 'ANSWERED' AND response_sec IS NOT NULL
  AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY question_type;
```

---

## 5. `notification_delivery_log` — 이메일·푸시 발송 추적

### 기존 로그의 한계
이메일과 FCM 발송 실패 시 `log.error`만 남기고 DB에 기록이 없다.
외부 서비스 장애를 사후에 파악할 수 없다.

### 답할 수 있는 질문
- 면접 결과 이메일 발송 성공률은?
- FCM 푸시 발송 실패율과 실패 원인은?
- 이메일 유형별(결과/오류/가입 환영) 발송량은?
- 발송 실패가 급증한 시점은? (외부 서비스 장애 탐지)

### DDL
```sql
CREATE TABLE notification_delivery_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT,
    channel     VARCHAR(10)  NOT NULL,  -- EMAIL | FCM
    type        VARCHAR(40)  NOT NULL,  -- WELCOME | INTERVIEW_RESULT | INTERVIEW_ERROR | WITHDRAW_CONFIRM
    ref_id      BIGINT,                 -- interview_id 등 관련 엔티티 ID
    success     BOOLEAN      NOT NULL,
    fail_reason VARCHAR(255),           -- 실패 시 에러 메시지 앞 255자
    created_at  DATETIME(3)  NOT NULL,

    INDEX idx_ndl_channel_type_created (channel, type, created_at),
    INDEX idx_ndl_success_created      (success, created_at),
    INDEX idx_ndl_created              (created_at)
);
```

### 발송 유형 정의
| type | channel | 발송 조건 | 기록 위치 |
|------|---------|----------|----------|
| `WELCOME` | EMAIL | 회원가입 완료 후 | `AwsSesEmailServiceImpl` |
| `INTERVIEW_RESULT` | EMAIL + FCM | FastAPI 콜백 결과 저장 완료 | `InterviewController.callback()` |
| `INTERVIEW_ERROR` | EMAIL | FastAPI 콜백 오류 시 | `InterviewController.callback()` |
| `WITHDRAW_CONFIRM` | EMAIL | 회원탈퇴 완료 후 | `AccountService.withdraw()` |

### 어드민 활용 쿼리
```sql
-- 채널·유형별 발송 성공률 (최근 7일)
SELECT
    channel, type,
    SUM(success)  AS ok,
    SUM(!success) AS fail,
    COUNT(*)      AS total,
    ROUND(SUM(success) / COUNT(*) * 100, 1) AS success_pct
FROM notification_delivery_log
WHERE created_at >= NOW() - INTERVAL 7 DAY
GROUP BY channel, type ORDER BY channel, type;

-- 최근 실패 원인 TOP 10
SELECT fail_reason, COUNT(*) AS cnt
FROM notification_delivery_log
WHERE success = FALSE AND created_at >= NOW() - INTERVAL 7 DAY
GROUP BY fail_reason ORDER BY cnt DESC LIMIT 10;
```

---

## 6. `credit_attempt_log` — 크레딧 결제 시도

### 기존 `credit_transaction`의 한계
성공한 거래만 기록된다.
크레딧 부족으로 402가 반환되는 상황 — 즉 유료 전환 가능성이 있는 유저 — 이 전혀 잡히지 않는다.

### 답할 수 있는 질문
- 크레딧 부족으로 막힌 사용자가 하루 몇 명인가? (충전 유도 캠페인 대상)
- 어떤 기능에서 크레딧 부족이 가장 많이 발생하는가?
- 크레딧 부족 후 실제 충전으로 전환하는 비율은?

### DDL
```sql
CREATE TABLE credit_attempt_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT       NOT NULL,
    reason      VARCHAR(50)  NOT NULL,  -- credit_transaction.reason 표준값 사용
    amount      INT          NOT NULL,  -- 필요 크레딧 수
    balance     INT          NOT NULL,  -- 시도 시점 잔액
    result      VARCHAR(20)  NOT NULL,  -- SUCCESS | INSUFFICIENT
    created_at  DATETIME(3)  NOT NULL,

    INDEX idx_cal_result_created   (result, created_at),
    INDEX idx_cal_account_created  (account_id, created_at),
    INDEX idx_cal_created          (created_at)
);
```

### 기록 위치
`CreditWalletServiceImpl.getCreditPayByAccountId()` — 성공/실패 분기 직후

### 어드민 활용 쿼리
```sql
-- 크레딧 부족 일별 유저 수 (충전 유도 캠페인 기준)
SELECT DATE(created_at) AS day, COUNT(DISTINCT account_id) AS blocked_users
FROM credit_attempt_log
WHERE result = 'INSUFFICIENT' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY day ORDER BY day DESC;

-- 기능별 크레딧 부족 발생 수
SELECT reason, COUNT(*) AS insufficient_cnt
FROM credit_attempt_log
WHERE result = 'INSUFFICIENT' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY reason ORDER BY insufficient_cnt DESC;

-- 크레딧 부족 → 충전 전환율 (7일 이내 충전)
SELECT
    COUNT(DISTINCT a.account_id)                          AS blocked_users,
    COUNT(DISTINCT ct.account_id)                         AS converted_users,
    ROUND(COUNT(DISTINCT ct.account_id)
          / NULLIF(COUNT(DISTINCT a.account_id),0) * 100, 1) AS conversion_pct
FROM credit_attempt_log a
LEFT JOIN credit_transaction ct
    ON ct.account_id = a.account_id
   AND ct.type = 'PURCHASE'
   AND ct.created_at BETWEEN a.created_at AND DATE_ADD(a.created_at, INTERVAL 7 DAY)
WHERE a.result = 'INSUFFICIENT'
  AND a.created_at >= NOW() - INTERVAL 30 DAY;
```

---

## 7. `review_log` — 리뷰 제출 이벤트

### 답할 수 있는 질문
- 면접 리뷰 vs 서비스 리뷰 제출 수는?
- 평점 분포 추이는?
- 리뷰를 남기는 유저의 면접 횟수 평균은?

### DDL
```sql
CREATE TABLE review_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT      NOT NULL,
    review_type VARCHAR(20) NOT NULL,  -- INTERVIEW | IPOTEN
    ref_id      BIGINT,               -- interview_id (INTERVIEW 타입일 때)
    rating      TINYINT     NOT NULL,  -- 1~5
    created_at  DATETIME(3) NOT NULL,

    INDEX idx_rl_type_created   (review_type, created_at),
    INDEX idx_rl_rating_created (rating, created_at),
    INDEX idx_rl_created        (created_at)
);
```

### 기록 위치
- `InterviewReviewServiceImpl.registerInterviewReview()` 완료 후
- `IpotenReviewServiceImpl.registerInterviewReview()` 완료 후

### 어드민 활용 쿼리
```sql
-- 유형별 주간 평균 평점
SELECT
    review_type,
    YEARWEEK(created_at) AS week,
    ROUND(AVG(rating), 2) AS avg_rating,
    COUNT(*) AS cnt
FROM review_log
WHERE created_at >= NOW() - INTERVAL 90 DAY
GROUP BY review_type, week ORDER BY review_type, week DESC;

-- 평점 분포
SELECT review_type, rating, COUNT(*) AS cnt
FROM review_log
WHERE created_at >= NOW() - INTERVAL 30 DAY
GROUP BY review_type, rating ORDER BY review_type, rating;
```

---

## 8. `credit_transaction.reason` 표준화

기존 `reason` 필드가 자유 문자열이면 집계 불가. 아래 값으로 고정한다.

| reason 값 | 설명 |
|-----------|------|
| `INTERVIEW_CREATE_TECHNICAL` | 기술면접 생성 |
| `INTERVIEW_CREATE_COMPANY` | 기업면접 생성 |
| `INTERVIEW_CREATE_PERSONAL` | 인성면접 생성 |
| `SIGNUP_BONUS` | 신규 가입 크레딧 지급 |
| `CHARGE_PURCHASE` | 유료 충전 |
| `CHARGE_BONUS_EVENT` | 이벤트 지급 |
| `REFUND` | 환불 |

```sql
-- 기능별 크레딧 소모량 (최근 30일)
SELECT reason, SUM(ABS(amount)) AS total_used, COUNT(*) AS usage_count
FROM credit_transaction
WHERE type IN ('USE', 'USAGE') AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY reason ORDER BY total_used DESC;
```

---

## 9. 전체 이벤트 기록 시점 정리

| 이벤트 | 기록 테이블 | event 값 | 기록 위치 |
|--------|------------|----------|----------|
| OAuth 완료, 신규 유저 임시 토큰 발급 | `account_lifecycle_log` | `SIGNUP_STARTED` | 각 OAuth 서비스 신규 분기 |
| 회원가입 완료 | `account_lifecycle_log` | `SIGNUP_COMPLETED` | `SignupServiceImpl.signup()` |
| 닉네임 변경 | `account_lifecycle_log` | `NICKNAME_UPDATED` | `AccountProfileServiceImp.updateNickname()` |
| 로그인 성공 | `user_session_log` | `LOGIN` | 각 OAuth 서비스 완료 후 |
| 로그아웃 | `user_session_log` | `LOGOUT` | `AuthenticationServiceImpl.logout()` |
| 회원탈퇴 | `user_session_log` + `account_lifecycle_log` | `WITHDRAW` | `AccountService.withdraw()` |
| 면접 생성 | `interview_event_log` | `CREATED` | `InterviewServiceImpl.createInterview()` |
| 질문 전달 | `interview_question_log` | `ASKED` | `InterviewServiceImpl.createInterview()` / `execute()` |
| 답변 제출 | `interview_question_log` | `ANSWERED` | `InterviewServiceImpl.execute()` |
| 면접 진행 | `interview_event_log` | `PROGRESSED` | `InterviewServiceImpl.execute()` |
| 면접 종료 | `interview_event_log` | `COMPLETED` / `ABORTED` | `InterviewServiceImpl.endInterview()` |
| FastAPI 콜백 수신 | `interview_event_log` | `CALLBACK_RECEIVED` | `InterviewController.callback()` |
| 결과 조회 | `interview_event_log` | `RESULT_VIEWED` | `InterviewController.getInterviewResult()` |
| 크레딧 결제 시도 | `credit_attempt_log` | `SUCCESS` / `INSUFFICIENT` | `CreditWalletServiceImpl.getCreditPayByAccountId()` |
| 크레딧 변동 | `credit_transaction` | (이미 존재) | `CreditWalletServiceImpl` |
| 이메일 발송 | `notification_delivery_log` | `EMAIL` | `AwsSesEmailServiceImpl` |
| FCM 푸시 발송 | `notification_delivery_log` | `FCM` | `FcmNotificationServiceImpl` |
| 면접 리뷰 제출 | `review_log` | `INTERVIEW` | `InterviewReviewServiceImpl` |
| 서비스 리뷰 제출 | `review_log` | `IPOTEN` | `IpotenReviewServiceImpl` |

---

## 10. 구현 방식

### 비동기 + 독립 트랜잭션

이벤트 로그 저장 실패가 핵심 비즈니스 로직에 영향을 주면 안 된다.

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewEventLogger {

    private final InterviewEventLogRepository repo;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(InterviewEventLog eventLog) {
        try {
            repo.save(eventLog);
        } catch (Exception e) {
            log.error("[EVENT-LOG] interview_event_log 저장 실패 - interviewId={}, event={}",
                      eventLog.getInterviewId(), eventLog.getEvent(), e);
        }
    }
}
```

### Spring Application Event 활용 (권장)

서비스 코드와 로깅 코드를 완전히 분리한다.

```java
// 이벤트 발행 (InterviewServiceImpl)
applicationEventPublisher.publishEvent(
    new InterviewCreatedEvent(interview.getId(), accountId, interviewType, plan, company, job, career)
);

// 이벤트 리스너 (로깅 전담)
@Async
@EventListener
public void onInterviewCreated(InterviewCreatedEvent e) {
    logger.record(InterviewEventLog.of(e, "CREATED"));
}
```

### 인덱스 원칙

| 목적 | 인덱스 패턴 |
|------|-----------|
| 기간 필터링 기본 | `created_at` 단독 |
| 분류별 × 기간 집계 | `(event, created_at)` 복합 |
| 특정 엔티티 추적 | `(interview_id)` 단독 |
| 개인 행동 이력 | `(account_id, created_at)` 복합 |

---

## 11. 어드민 대시보드 예상 지표

| 카테고리 | 지표 | 원천 테이블 |
|----------|------|-----------|
| **사용자** | DAU / WAU / MAU | `user_session_log` |
| **사용자** | 신규 가입자 추이 | `account` + `account_lifecycle_log` |
| **사용자** | 가입 전환율 (OAuth 시작 → 완료) | `account_lifecycle_log` |
| **사용자** | provider별 활성 비율 | `user_session_log` |
| **사용자** | 웹 vs 모바일 비율 | `user_session_log` |
| **면접** | 회사별 면접 생성 Top N | `interview_event_log` |
| **면접** | 유형·플랜별 완료율 | `interview_event_log` |
| **면접** | 직군별 면접 수 | `interview_event_log` |
| **면접** | 질문 번호별 이탈 지점 | `interview_question_log` |
| **면접** | FastAPI 콜백 실패율 | `interview_event_log` |
| **면접** | 결과 조회까지 소요 시간 | `interview_event_log` (COMPLETED vs RESULT_VIEWED) |
| **크레딧** | 기능별 크레딧 소모량 | `credit_transaction` (reason 표준화 후) |
| **크레딧** | 크레딧 부족 일별 유저 수 | `credit_attempt_log` |
| **크레딧** | 부족 → 충전 전환율 | `credit_attempt_log` + `credit_transaction` |
| **알림** | 이메일·FCM 발송 성공률 | `notification_delivery_log` |
| **알림** | 발송 실패 원인 Top N | `notification_delivery_log` |
| **리뷰** | 면접·서비스 리뷰 평균 평점 추이 | `review_log` |
| **리뷰** | 평점 분포 | `review_log` |

---

## 12. 구현 우선순위

| 순서 | 대상 | 이유 |
|------|------|------|
| 1 | `credit_transaction.reason` 표준화 | 코드 변경 최소, 기존 데이터 분석 즉시 개선 |
| 2 | `interview_event_log` | 핵심 서비스 지표, 어드민이 제일 먼저 요구 |
| 3 | `user_session_log` | DAU/리텐션은 서비스 건강도 기본 지표 |
| 4 | `credit_attempt_log` | 크레딧 부족 = 잠재 매출, 충전 캠페인 근거 |
| 5 | `notification_delivery_log` | 인프라 안정성 모니터링 |
| 6 | `interview_question_log` | 제품 개선 인사이트 (질문별 이탈 분석) |
| 7 | `account_lifecycle_log` | 가입 퍼널 개선 데이터 |
| 8 | `review_log` | 중요하지만 분석 빈도 낮음 |
