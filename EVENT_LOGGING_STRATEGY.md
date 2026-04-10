# I-Poten 이벤트 로깅 전략 (DB 저장형)

> 이 문서는 **어드민 대시보드 가시화 및 비즈니스 데이터 분석**을 위해
> DB에 저장하는 이벤트 로그 설계를 다룬다.
> slf4j 애플리케이션 로그와는 다른 개념이다.

---

## 현재 이미 존재하는 이벤트 로그

새로 만들기 전에 기존 테이블을 파악한다.

| 테이블 | 주요 컬럼 | 답할 수 있는 질문 |
|--------|-----------|------------------|
| `term_search_log` | actorKey, queryNorm, resultCount, isZero, latencyMs | 인기 검색어, 검색 결과 없는 키워드, 검색 latency 분포 |
| `wordbook_log` | accountId, eventType, wordbookId, termId, amount | 단어장 이벤트별 빈도, 사용자별 활동량 |
| `credit_transaction` | type, amount, balanceAfter, reason | 기능별 크레딧 소모량, 충전/사용 패턴, 수익 추이 |
| `interview` | account, interviewType, plan, sender, isFinished, createdAt | 회사별/유형별 면접 생성 수, 플랜 분포 |
| `account` | loginType, createdAt | 소셜 로그인 provider별 신규 가입자 추이 |

**`credit_transaction.reason`** 을 잘 정의하면 기능별 사용 통계를 대부분 커버할 수 있다.

---

## 새로 만들어야 할 이벤트 로그 테이블

기존 테이블로 답할 수 없는 분석 질문들을 기준으로 설계한다.

---

### 1. `user_session_log` — 사용자 세션 이벤트

**답할 수 있는 질문:**
- DAU / WAU / MAU 는 몇 명인가?
- 소셜 로그인 provider별 실제 활성 사용자 비율은?
- 웹 vs 모바일 사용 비율은?
- 특정 날짜 이후 재방문율은? (리텐션)

```sql
CREATE TABLE user_session_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT       NOT NULL,
    action      VARCHAR(20)  NOT NULL,   -- LOGIN | LOGOUT | TOKEN_REFRESH
    provider    VARCHAR(20),             -- KAKAO | GOOGLE | NAVER | GITHUB | APPLE | META
    platform    VARCHAR(20),             -- WEB | MOBILE
    created_at  DATETIME(3)  NOT NULL,

    INDEX idx_usl_account_created  (account_id, created_at),
    INDEX idx_usl_created          (created_at),
    INDEX idx_usl_action_created   (action, created_at)
);
```

**어드민 활용 예시:**
```sql
-- DAU (일별 활성 사용자 수)
SELECT DATE(created_at) AS day, COUNT(DISTINCT account_id) AS dau
FROM user_session_log
WHERE action = 'LOGIN'
GROUP BY day ORDER BY day DESC;

-- provider별 활성 비율 (최근 30일)
SELECT provider, COUNT(DISTINCT account_id) AS active_users
FROM user_session_log
WHERE action = 'LOGIN' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY provider;
```

---

### 2. `interview_event_log` — 면접 이벤트

**기존 `interview` 테이블의 한계:**
- 생성 시점만 기록됨
- 완료/중단 시점, 소요 시간, 점수를 한 번에 추적 불가

**답할 수 있는 질문:**
- 회사별 면접 생성 수 / 완료 수는?
- 면접 유형(TECHNICAL / COMPANY / PERSONAL)별 사용량은?
- 플랜(FREE / NORMAL / PREMIUM)별 비율은?
- 면접 완료율은? (생성 대비 완료 비율)
- 평균 면접 점수 추이는?
- 어떤 회사 면접이 가장 많이 생성되는가?
- 면접 1회당 평균 소요 시간은?

```sql
CREATE TABLE interview_event_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT       NOT NULL,
    interview_id    BIGINT       NOT NULL,
    event           VARCHAR(30)  NOT NULL,   -- CREATED | PROGRESS | COMPLETED | ABORTED
    interview_type  VARCHAR(20),             -- TECHNICAL | COMPANY | PERSONAL
    plan            VARCHAR(20),             -- FREE | NORMAL | PREMIUM
    company         VARCHAR(100),            -- sender 값 (회사명)
    score           INT,                     -- 채점 완료 시점에만
    duration_sec    INT,                     -- COMPLETED 시점에만 (생성~완료 초)
    created_at      DATETIME(3)  NOT NULL,

    INDEX idx_iel_account_created      (account_id, created_at),
    INDEX idx_iel_event_created        (event, created_at),
    INDEX idx_iel_company_event        (company, event, created_at),
    INDEX idx_iel_type_event_created   (interview_type, event, created_at)
);
```

**어드민 활용 예시:**
```sql
-- 회사별 면접 생성 수 (상위 20개)
SELECT company, COUNT(*) AS created_count
FROM interview_event_log
WHERE event = 'CREATED' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY company ORDER BY created_count DESC LIMIT 20;

-- 면접 유형별 완료율
SELECT
    interview_type,
    SUM(event = 'CREATED')   AS created,
    SUM(event = 'COMPLETED') AS completed,
    ROUND(SUM(event = 'COMPLETED') / SUM(event = 'CREATED') * 100, 1) AS completion_pct
FROM interview_event_log
WHERE created_at >= NOW() - INTERVAL 30 DAY
GROUP BY interview_type;

-- 주간 평균 점수 추이
SELECT YEARWEEK(created_at) AS week, ROUND(AVG(score), 1) AS avg_score
FROM interview_event_log
WHERE event = 'COMPLETED' AND score IS NOT NULL
GROUP BY week ORDER BY week DESC LIMIT 12;
```

---

### 3. `quiz_play_log` — 퀴즈 플레이 이벤트

**답할 수 있는 질문:**
- 퀴즈 유형(CHOICE / OX / INITIALS / DAILY)별 일별 참여자 수는?
- 유형별 평균 정답률은?
- 퀴즈 완료율은? (세션 시작 대비 제출 비율)
- 어떤 유형에서 가장 많이 이탈하는가?
- 오늘의 퀴즈 참여율 추이는?

```sql
CREATE TABLE quiz_play_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id      BIGINT       NOT NULL,
    session_id      BIGINT       NOT NULL,
    quiz_set_type   VARCHAR(30)  NOT NULL,   -- CHOICE | OX | INITIALS | DAILY_GENERAL
    event           VARCHAR(20)  NOT NULL,   -- STARTED | SUBMITTED | EXPIRED
    correct_count   INT,                     -- SUBMITTED 시점에만
    total_count     INT,
    score           INT,
    created_at      DATETIME(3)  NOT NULL,

    INDEX idx_qpl_account_created       (account_id, created_at),
    INDEX idx_qpl_type_event_created    (quiz_set_type, event, created_at),
    INDEX idx_qpl_created               (created_at)
);
```

**어드민 활용 예시:**
```sql
-- 유형별 평균 정답률
SELECT
    quiz_set_type,
    ROUND(AVG(correct_count / total_count * 100), 1) AS avg_correct_pct,
    COUNT(*) AS session_count
FROM quiz_play_log
WHERE event = 'SUBMITTED' AND total_count > 0
  AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY quiz_set_type;

-- 일별 오늘의 퀴즈 참여자 수
SELECT DATE(created_at) AS day, COUNT(DISTINCT account_id) AS participants
FROM quiz_play_log
WHERE quiz_set_type = 'DAILY_GENERAL' AND event = 'STARTED'
GROUP BY day ORDER BY day DESC LIMIT 30;
```

---

### 4. `feature_usage_log` — 기능별 사용 현황

크레딧 소모가 없는 무료 기능(ebook 조회, PDF 다운로드 등)의 사용량을 추적한다.
크레딧이 필요한 기능은 `credit_transaction.reason` 으로 이미 커버되므로 중복 기록하지 않는다.

**답할 수 있는 질문:**
- PDF 다운로드를 가장 많이 하는 사용자는?
- 전자책 뷰어 일별 사용량은?
- 어떤 기능이 가장 많이 쓰이는가?

```sql
CREATE TABLE feature_usage_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    account_id  BIGINT       NOT NULL,
    feature     VARCHAR(50)  NOT NULL,   -- 아래 Feature 목록 참고
    ref_id      BIGINT,                  -- 관련 엔티티 ID (ebook_id, wordbook_id 등)
    created_at  DATETIME(3)  NOT NULL,

    INDEX idx_ful_feature_created   (feature, created_at),
    INDEX idx_ful_account_created   (account_id, created_at),
    INDEX idx_ful_created           (created_at)
);
```

**Feature 값 목록:**
| 값 | 설명 |
|----|------|
| `EBOOK_VIEW` | 전자책 PDF 스트리밍 |
| `PDF_EXPORT` | 단어장 PDF 다운로드 |
| `DAILY_QUIZ_START` | 오늘의 퀴즈 시작 |
| `TTS_PERSONALITY` | 인성면접 TTS 재생 |

---

## credit_transaction.reason 표준화

기존 `credit_transaction` 의 `reason` 필드가 자유 문자열이면 집계가 어렵다.
**아래 값으로 표준화**하면 기능별 크레딧 소모 분석이 가능하다.

| reason 값 | 설명 |
|-----------|------|
| `INTERVIEW_CREATE_TECHNICAL` | 기술면접 생성 |
| `INTERVIEW_CREATE_COMPANY` | 기업면접 생성 |
| `INTERVIEW_CREATE_PERSONAL` | 인성면접 생성 |
| `INTERVIEW_REVIEW` | 면접 리뷰 |
| `IPOTEN_REVIEW` | 아이포텐 리뷰 |
| `CHARGE_PURCHASE` | 유료 충전 |
| `CHARGE_BONUS` | 이벤트 지급 |
| `REFUND` | 환불 |

**활용 쿼리:**
```sql
-- 기능별 크레딧 소모 집계 (최근 30일)
SELECT reason, SUM(ABS(amount)) AS total_used, COUNT(*) AS usage_count
FROM credit_transaction
WHERE type = 'USE' AND created_at >= NOW() - INTERVAL 30 DAY
GROUP BY reason ORDER BY total_used DESC;
```

---

## 전체 이벤트 기록 시점 정리

| 이벤트 | 기록 테이블 | 기록 시점 | 담당 서비스 |
|--------|------------|----------|------------|
| 로그인 성공 | `user_session_log` | OAuth 핸들러 완료 후 | 각 OAuth Service |
| 토큰 갱신 | `user_session_log` | RefreshTokenService.rotate() |  MobileAuthController |
| 회원탈퇴 | `user_session_log` (WITHDRAW) | AccountService.withdraw() | AccountService |
| 면접 생성 | `interview_event_log` (CREATED) | InterviewService 완료 후 | InterviewService |
| 면접 완료 | `interview_event_log` (COMPLETED) | FastAPI 콜백 수신 후 | InterviewController.callback() |
| 퀴즈 세션 시작 | `quiz_play_log` (STARTED) | 세션 생성 완료 후 | QuizSessionGenerator |
| 퀴즈 세션 제출 | `quiz_play_log` (SUBMITTED) | 채점 완료 후 | QuizSessionAnswerService |
| 크레딧 변동 | `credit_transaction` | CreditWalletService (이미 존재) | CreditWalletService |
| PDF 다운로드 | `feature_usage_log` (PDF_EXPORT) | PDF 생성 완료 후 | WordbookPdfExportService |
| 전자책 조회 | `feature_usage_log` (EBOOK_VIEW) | 스트림 시작 시 | EbookService |
| 용어 검색 | `term_search_log` | 검색 완료 후 (이미 존재) | TermSearchService |
| 단어장 이벤트 | `wordbook_log` | 각 이벤트 시점 (이미 존재) | WordbookService |

---

## 구현 방식

### 비동기 저장 권장

이벤트 로그 저장이 실패해도 **핵심 비즈니스 로직에 영향을 주면 안 된다**.
`@Async` 로 비동기 저장하고, 실패 시 slf4j ERROR 로그로 대체한다.

```java
@Async
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void record(InterviewEventLog log) {
    try {
        interviewEventLogRepository.save(log);
    } catch (Exception e) {
        log.error("[EVENT-LOG] 저장 실패 - {}", log, e);
    }
}
```

### 인덱스 원칙

- **`created_at` 단독 인덱스** — 기간 필터링 기본
- **`(분류컬럼, created_at)` 복합 인덱스** — "유형별 × 기간" 집계
- **`account_id`는 분석용이 아니면 인덱스 불필요** — 사용자 개인 조회가 필요할 때만 추가

---

## 어드민 대시보드 예상 지표

| 카테고리 | 지표 | 원천 테이블 |
|----------|------|------------|
| **사용자** | DAU / WAU / MAU | `user_session_log` |
| **사용자** | 신규 가입자 추이 | `account` |
| **사용자** | provider별 가입/활동 비율 | `account` + `user_session_log` |
| **면접** | 회사별 면접 생성 수 (Top N) | `interview_event_log` |
| **면접** | 유형별 완료율 | `interview_event_log` |
| **면접** | 주간 평균 점수 추이 | `interview_event_log` |
| **면접** | 플랜별 사용 분포 | `interview_event_log` |
| **퀴즈** | 유형별 정답률 | `quiz_play_log` |
| **퀴즈** | 오늘의 퀴즈 일별 참여자 | `quiz_play_log` |
| **퀴즈** | 세션 완료율 | `quiz_play_log` |
| **크레딧** | 기능별 크레딧 소모량 | `credit_transaction` |
| **크레딧** | 충전 / 사용 / 환불 추이 | `credit_transaction` |
| **검색** | 인기 검색어 Top N | `term_search_log` |
| **검색** | 결과 없는 검색어 (콘텐츠 공백 발견) | `term_search_log` |
| **기능** | PDF 다운로드 / 전자책 조회 수 | `feature_usage_log` |

---

## 구현 우선순위

| 순서 | 대상 | 이유 |
|------|------|------|
| 1 | `credit_transaction.reason` 표준화 | 기존 데이터 오염 없이 분석 정확도 즉시 개선 |
| 2 | `interview_event_log` | 핵심 서비스 + 어드민이 가장 먼저 보고 싶은 지표 |
| 3 | `user_session_log` | DAU/리텐션은 서비스 건강도 지표의 기본 |
| 4 | `quiz_play_log` | 주요 기능 참여율 추적 |
| 5 | `feature_usage_log` | 보조 기능 분석, 우선순위 낮음 |
