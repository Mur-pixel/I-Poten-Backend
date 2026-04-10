# 프론트엔드 이벤트 로깅 전략

---

## 1. 추적할 이벤트 정의

이벤트는 3가지 계층으로 나눈다.

### Layer 1 — 페이지 뷰 (Page View)
사용자가 특정 페이지에 진입할 때 발생.

| event | page | 설명 |
|-------|------|------|
| `PAGE_VIEW` | `LANDING` | 랜딩페이지 진입 |
| `PAGE_VIEW` | `LOGIN` | 로그인 페이지 진입 |
| `PAGE_VIEW` | `SIGNUP` | 회원가입 페이지 진입 |
| `PAGE_VIEW` | `INTERVIEW` | 면접 서비스 페이지 진입 |
| `PAGE_VIEW` | `QUIZ` | 퀴즈 페이지 진입 |
| `PAGE_VIEW` | `WORDBOOK` | 단어장 페이지 진입 |
| `PAGE_VIEW` | `TERM_SEARCH` | 용어 검색 페이지 진입 |
| `PAGE_VIEW` | `EBOOK` | 전자책 페이지 진입 |
| `PAGE_VIEW` | `MY_PAGE` | 마이페이지 진입 |

### Layer 2 — 클릭 이벤트 (Click)
특정 버튼/액션을 클릭할 때 발생.

| event | target | 설명 |
|-------|--------|------|
| `CLICK` | `LOGIN_KAKAO` | 카카오 로그인 버튼 클릭 |
| `CLICK` | `LOGIN_GOOGLE` | 구글 로그인 버튼 클릭 |
| `CLICK` | `LOGIN_NAVER` | 네이버 로그인 버튼 클릭 |
| `CLICK` | `SIGNUP_AGREE` | 약관 동의 완료 |
| `CLICK` | `SIGNUP_SUBMIT` | 회원가입 제출 버튼 클릭 |
| `CLICK` | `NAV_INTERVIEW` | 네브바 면접 클릭 |
| `CLICK` | `NAV_QUIZ` | 네브바 퀴즈 클릭 |
| `CLICK` | `NAV_WORDBOOK` | 네브바 단어장 클릭 |
| `CLICK` | `INTERVIEW_START` | 면접 시작 버튼 클릭 |
| `CLICK` | `CREDIT_CHARGE` | 크레딧 충전 버튼 클릭 |

### Layer 3 — 퍼널 이벤트 (Funnel)
전환 흐름을 추적하는 단계별 이벤트.

```
[회원가입 퍼널]
LANDING 진입 → 로그인 버튼 클릭 → 소셜 로그인 선택 → 약관 동의 → 가입 완료

[면접 퍼널]
면접 페이지 진입 → 회사/직무 입력 → 크레딧 확인 → 면접 시작 → 면접 완료
```

---

## 2. 구현 방법

### 2-1. 이벤트 수집기 (Logger 모듈) 중앙화

이벤트 추적 코드를 컴포넌트마다 흩어두면 유지보수가 불가능해진다.
**반드시 하나의 모듈로 중앙화**한다.

```typescript
// lib/logger.ts

const SESSION_KEY = 'ipoten_session_id';

function getSessionId(): string {
  let id = sessionStorage.getItem(SESSION_KEY);
  if (!id) {
    id = crypto.randomUUID();
    sessionStorage.setItem(SESSION_KEY, id);
  }
  return id;
}

interface EventPayload {
  event: string;
  page?: string;
  target?: string;
  meta?: Record<string, unknown>;
}

export async function logEvent(payload: EventPayload): Promise<void> {
  // 이벤트 로깅 실패가 UX를 막으면 절대 안 됨 → fire-and-forget
  navigator.sendBeacon('/api/events', JSON.stringify({
    ...payload,
    sessionId: getSessionId(),
    timestamp: new Date().toISOString(),
    // accountId는 백엔드에서 userToken 쿠키로 식별 — 프론트에서 안 보냄
  }));
}
```

```typescript
// 사용 예시 — 컴포넌트에서
import { logEvent } from '@/lib/logger';

// 페이지 진입 시
logEvent({ event: 'PAGE_VIEW', page: 'LANDING' });

// 버튼 클릭 시
<button onClick={() => {
  logEvent({ event: 'CLICK', target: 'LOGIN_KAKAO', page: 'LOGIN' });
  handleKakaoLogin();
}}>
  카카오 로그인
</button>
```

### 2-2. SPA 라우팅에서 페이지 뷰 자동 추적

React Router / Next.js 는 페이지 이동 시 HTML 요청이 없어서
`window.location` 변화를 감지해야 한다.

**Next.js App Router:**
```typescript
// app/layout.tsx 또는 전역 Provider
'use client';
import { usePathname } from 'next/navigation';
import { useEffect } from 'react';
import { logEvent } from '@/lib/logger';

const PATH_TO_PAGE: Record<string, string> = {
  '/': 'LANDING',
  '/login': 'LOGIN',
  '/signup': 'SIGNUP',
  '/interview': 'INTERVIEW',
  '/quiz': 'QUIZ',
  '/wordbook': 'WORDBOOK',
};

export function PageViewTracker() {
  const pathname = usePathname();

  useEffect(() => {
    const page = PATH_TO_PAGE[pathname] ?? pathname;
    logEvent({ event: 'PAGE_VIEW', page });
  }, [pathname]);

  return null;
}
```

**React Router:**
```typescript
// 전역 위치에서
const location = useLocation();
useEffect(() => {
  logEvent({ event: 'PAGE_VIEW', page: location.pathname });
}, [location.pathname]);
```

### 2-3. 백엔드 수집 API

```
POST /api/events
Content-Type: application/json

{
  "event": "PAGE_VIEW",
  "page": "LANDING",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-03-19T14:22:01.000Z"
}
```

- `accountId` 는 **프론트에서 전송하지 않는다** — 백엔드에서 `userToken` 쿠키로 식별
- 비회원은 `accountId = null` 로 저장, `sessionId` 로만 추적
- 이 API는 `@PublicEndpoint` (인증 없이 수신 가능해야 함)

---

## 3. 문제점 및 주의사항

### 3-1. 광고 차단기 (Ad Blocker) 문제

브라우저 확장 프로그램(uBlock, AdBlock 등)이 외부 트래킹 스크립트를 차단한다.
자체 도메인의 `/api/events` 엔드포인트는 차단 확률이 낮지만 **완전히 피할 수는 없다**.

**영향:** 실제 방문자 대비 5~30% 데이터 유실 가능
**대응:** 수집된 데이터를 절대값이 아닌 **상대적 트렌드**로만 해석

---

### 3-2. 이중 발화 (Duplicate Events) 문제

React StrictMode, 라우터 동작 방식에 따라 `useEffect` 가 두 번 실행되어
같은 이벤트가 중복 저장될 수 있다.

```typescript
// 잘못된 방식 — StrictMode에서 2번 발화
useEffect(() => {
  logEvent({ event: 'PAGE_VIEW', page: 'LANDING' });
}, []);

// 대응 — 짧은 시간 내 동일 이벤트 중복 방어
const lastSentRef = useRef<string | null>(null);

useEffect(() => {
  const key = `PAGE_VIEW:${pathname}`;
  if (lastSentRef.current === key) return;
  lastSentRef.current = key;
  logEvent({ event: 'PAGE_VIEW', page: pathname });
}, [pathname]);
```

---

### 3-3. 이벤트 폭발 (Event Flood) 문제

스크롤, 마우스 무브 같은 빈번한 이벤트를 그대로 전송하면
DB에 수천만 건이 쌓여 분석이 불가능해진다.

**원칙:**
- 페이지뷰, 버튼 클릭 → 즉시 전송
- 스크롤 깊이, 마우스 호버 → **추적하지 않거나 debounce**
- 검색 입력 중 → 입력 완료(엔터/클릭) 시점에만 전송

---

### 3-4. 세션 ID 관리 문제

`sessionStorage` 는 **탭 단위**로 격리된다.
같은 사용자가 탭을 여러 개 열면 sessionId 가 다르게 생성된다.

| 저장소 | 탭 공유 | 브라우저 종료 후 | 적합한 용도 |
|--------|---------|-----------------|------------|
| `sessionStorage` | X | 삭제 | 단일 탭 세션 추적 |
| `localStorage` | O | 유지 | 재방문자 식별 |
| 쿠키 | O | 설정에 따라 | 백엔드 연동 필요 시 |

**재방문자 추적이 필요하다면:**
```typescript
// localStorage 기반 persistent device ID
const DEVICE_KEY = 'ipoten_device_id';
function getDeviceId(): string {
  let id = localStorage.getItem(DEVICE_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(DEVICE_KEY, id);
  }
  return id;
}
```

---

### 3-5. 비회원 → 회원 전환 시 세션 연결 문제

비회원일 때 `sessionId` 로 추적하다가 회원가입을 완료하면,
그 이전 행동 로그(랜딩 방문, 버튼 클릭)와 `accountId` 를 연결할 수 없다.

**대응:** 회원가입 완료 API 요청 시 `sessionId` 를 함께 전송
```typescript
// 회원가입 완료 시
await fetch('/api/account/signup', {
  body: JSON.stringify({
    ...formData,
    sessionId: getSessionId(), // 비회원 세션 연결용
  })
});
```
백엔드에서 `sessionId` 로 이전 이벤트를 `accountId` 에 연결할 수 있다.

---

### 3-6. 개인정보 및 법적 문제

행동 데이터 수집은 **개인정보보호법** 및 국내 규정 적용 대상이다.

| 항목 | 내용 |
|------|------|
| **고지 의무** | 이벤트 수집 사실을 개인정보처리방침에 명시해야 함 |
| **IP 저장 주의** | IP 주소는 개인정보로 분류됨 — 저장 시 암호화 또는 익명화 |
| **비회원 세션 ID** | 그 자체로는 개인정보 아님, 단 `accountId` 와 결합 시 해당 |
| **보관 기간** | 수집 목적 달성 후 파기 계획 수립 필요 |
| **제3자 제공** | GA4 등 해외 서버 전송 시 국외 이전 고지 필요 |

---

### 3-7. 네트워크 실패 시 데이터 유실 문제

`fetch` 는 페이지 이동 직전에 취소될 수 있다.
특히 로그인 버튼 클릭 → 즉시 리다이렉트되는 경우 이벤트 전송이 중단된다.

```typescript
// 잘못된 방식
await fetch('/api/events', { ... }); // 리다이렉트 시 취소됨

// 올바른 방식 — sendBeacon은 페이지 이동 후에도 전송 보장
navigator.sendBeacon('/api/events', JSON.stringify(payload));
```

`sendBeacon` 은 브라우저가 백그라운드에서 전송을 보장한다.
단, POST body만 지원하고 응답을 받을 수 없으므로 fire-and-forget 방식에만 적합.

---

### 3-8. 로컬/개발 환경 오염 문제

개발 중 클릭, 페이지 이동이 모두 DB에 쌓이면 분석 데이터가 오염된다.

```typescript
// lib/logger.ts
export async function logEvent(payload: EventPayload): Promise<void> {
  if (process.env.NODE_ENV !== 'production') {
    console.debug('[EVENT]', payload); // 개발 환경에선 콘솔만
    return;
  }
  navigator.sendBeacon('/api/events', JSON.stringify({ ... }));
}
```

---

## 4. 아키텍처 관점

### 자체 구현 vs GA4 비교

| 항목 | 자체 구현 (백엔드 API) | GA4 / Mixpanel |
|------|----------------------|----------------|
| 어드민 직접 연동 | 자유롭게 가능 | API 연동 필요 |
| 구현 공수 | 높음 | 낮음 (스크립트 삽입) |
| 데이터 소유권 | 완전 보유 | 외부 서버 |
| 광고 차단기 영향 | 낮음 | 높음 |
| 개인정보 국외 이전 | 해당 없음 | 고지 의무 발생 |
| 실시간 대시보드 | 직접 구현 | 기본 제공 |
| 퍼널 분석 | 직접 쿼리 | 기본 제공 |

**자체 어드민 대시보드를 만들 계획이라면 → 자체 구현**
**빠르게 인사이트만 얻고 싶다면 → GA4**

### 권장 구조 (자체 구현 시)

```
[프론트엔드]
  PageViewTracker (전역, 라우터 변화 감지)
  + logEvent() 유틸
        │
        │ POST /api/events  (sendBeacon)
        ▼
[백엔드]
  EventController (@PublicEndpoint)
        │
        │ @Async 비동기 저장
        ▼
  page_event_log 테이블
        │
        ▼
  [어드민 대시보드] 집계 쿼리
```

---

## 5. 구현 체크리스트

- [ ] `lib/logger.ts` — 중앙화된 이벤트 수집기 구현
- [ ] `sessionId` — sessionStorage 기반 세션 ID 생성
- [ ] `deviceId` — localStorage 기반 재방문자 식별 (필요 시)
- [ ] `PageViewTracker` — 라우터 변화 감지 전역 컴포넌트
- [ ] `sendBeacon` 사용 — 리다이렉트 전 이벤트 유실 방지
- [ ] 개발 환경 분기 — `NODE_ENV !== 'production'` 시 콘솔 출력만
- [ ] 중복 발화 방지 — StrictMode 대응
- [ ] 백엔드 `POST /api/events` — `@PublicEndpoint`, 비동기 저장
- [ ] 개인정보처리방침 — 행동 데이터 수집 항목 명시
- [ ] 회원가입 시 `sessionId` 전달 — 비회원 세션 연결

---

## 6. 어드민에서 답할 수 있는 질문 (수집 후)

```sql
-- 랜딩 → 회원가입 전환율
SELECT
  COUNT(DISTINCT CASE WHEN page = 'LANDING' THEN session_id END)  AS landing_sessions,
  COUNT(DISTINCT CASE WHEN page = 'SIGNUP'  THEN session_id END)  AS signup_page_sessions,
  COUNT(DISTINCT CASE WHEN page = 'SIGNUP' AND account_id IS NOT NULL THEN session_id END) AS signup_completed
FROM page_event_log
WHERE event = 'PAGE_VIEW' AND created_at >= NOW() - INTERVAL 30 DAY;

-- 네브바별 클릭 수
SELECT target, COUNT(*) AS click_count
FROM page_event_log
WHERE event = 'CLICK' AND target LIKE 'NAV_%'
  AND created_at >= NOW() - INTERVAL 7 DAY
GROUP BY target ORDER BY click_count DESC;

-- 소셜 로그인 버튼 클릭 비율
SELECT target, COUNT(*) AS attempts
FROM page_event_log
WHERE event = 'CLICK' AND target LIKE 'LOGIN_%'
GROUP BY target ORDER BY attempts DESC;
```
