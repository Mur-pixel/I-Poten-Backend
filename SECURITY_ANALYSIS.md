# I-Poten Backend - 보안 구조 및 CSRF 정책 분석

> 작성일: 2026-03-19
> 대상: `/Users/choehyeonsu/back/I-Poten-Backend` (Spring Boot 3.5.3, Java 17)

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프레임워크 | Spring Boot 3.5.3 |
| Java 버전 | 17 |
| 인증 방식 | 커스텀 쿠키 기반 (UUID 토큰 + Redis) |
| Spring Security | **미사용** |
| API 스타일 | REST API (전체 39개 컨트롤러 모두 `@RestController`) |
| 서버사이드 렌더링 | 없음 (Thymeleaf/JSP 등 미사용) |

---

## 2. 인증 구조

### 2-1. 토큰 종류

| 토큰 | 저장소 | 만료 | 용도 |
|------|--------|------|------|
| `userToken` | Redis | 6시간 | 일반 사용자 인증 |
| `temporaryToken` (`Temporary_` prefix) | Redis | 5분 | 신규 가입 중간 단계 |
| `temporaryAdminToken` | Redis | 별도 | 관리자 인증 |
| Refresh Token | DB (JPA) | 별도 | 모바일 앱 토큰 갱신 |

### 2-2. OAuth2 지원 프로바이더

Google / Kakao / Naver / GitHub / Apple / Meta (Facebook)

### 2-3. 인증 흐름 (OAuth2 예시)

```
1. GET /authentication/google/link         → 구글 인가 URL 반환
2. GET /authentication/google/login?code=  → 코드 교환 → 유저 정보 조회
3. 신규 유저: temporaryToken 발급 (5분)
   기존 유저: userToken 발급 → HttpOnly 쿠키 Set-Cookie
4. GET /api/authentication/token/verification → Redis 조회 → 유저 정보 반환
```

### 2-4. 모바일 인증 (Refresh Token)

```
POST /api/mobile/auth/register   → Refresh Token 등록
POST /api/mobile/auth/refresh    → 토큰 갱신 (rotation)
POST /api/mobile/auth/logout     → 토큰 폐기
```

---

## 3. CSRF 토큰 정책

### 결론: **CSRF 토큰 도입 불필요**

이유는 아래와 같습니다.

### 3-1. Spring Security 미사용

- `spring-boot-starter-security` 의존성 없음
- Spring Security의 CSRF 필터 자체가 동작하지 않음
- 별도의 CSRF 토큰 생성/검증 메커니즘도 없음

### 3-2. REST API 전용 서버

- 39개 컨트롤러 전부 `@RestController`
- HTML Form POST (브라우저의 기본 제출 방식)를 사용하는 엔드포인트 없음
- CSRF 공격의 주요 벡터인 폼 기반 상태 변경 요청이 존재하지 않음

### 3-3. SameSite 쿠키 속성으로 CSRF 방어 중

CSRF 토큰 없이도 쿠키의 `SameSite` 속성으로 동등한 수준의 보호가 이루어지고 있습니다.

| 쿠키 | SameSite 설정 | 이유 |
|------|--------------|------|
| 일반 `userToken` (웹) | `Strict` | 가장 강력한 보호 (동일 사이트 요청만 허용) |
| `temporaryAdminToken` (관리자) | `Lax` | 일반적인 교차 사이트 링크 허용 |
| `userToken` (GitHub OAuth) | `None; Secure` | 리다이렉트 플로우 특성상 불가피 |

**SameSite=Strict** 설정 시 다른 도메인에서 발생한 요청에는 쿠키가 전송되지 않으므로, CSRF 토큰 없이도 CSRF 공격을 차단합니다.

### 3-4. HttpOnly 쿠키

- 모든 인증 쿠키에 `HttpOnly` 적용
- JavaScript에서 쿠키 접근 불가 → XSS를 통한 토큰 탈취 차단

---

## 4. CORS 설정

**파일 위치:** `src/main/java/com/cygnus/ipoten/config/CorsConfig.java`

| 항목 | 설정값 |
|------|--------|
| 적용 경로 | `/**` (전체) |
| 허용 Origin | 환경변수 `CORS_ALLOWED_ORIGINS` |
| 허용 메서드 | GET, POST, PUT, DELETE, PATCH, OPTIONS |
| 허용 헤더 | `*` (전체) |
| Credentials | `true` (쿠키 전송 허용) |
| 노출 헤더 | `Content-Disposition`, `Ebook-*` 커스텀 헤더들 |
| Max Age | 3600초 |

> `allowCredentials(true)` + `SameSite=Strict` 조합으로 쿠키가 동일 출처 요청에서만 동작하도록 설계됨

---

## 5. 현재 보안 구조 요약

```
[클라이언트] ──── HTTPS ────► [Spring Boot REST API]
                                       │
                      ┌────────────────┼────────────────┐
                      ▼                ▼                ▼
                  [Redis]           [RDB (JPA)]      [OAuth2 Provider]
                 userToken       RefreshToken         Google/Kakao/...
                (6시간 TTL)       (모바일 전용)
```

**방어 레이어:**
1. `SameSite=Strict` → CSRF 방어
2. `HttpOnly` → XSS 토큰 탈취 방어
3. `Secure` → HTTPS 전송 강제
4. Redis TTL → 토큰 자동 만료
5. CORS Origin 제한 → 허가된 출처만 접근

---

## 6. 보완이 필요한 항목 (선택적 고려사항)

| 항목 | 현황 | 권고사항 |
|------|------|---------|
| Rate Limiting | 미적용 | 인증 엔드포인트에 요청 횟수 제한 고려 |
| GitHub OAuth 쿠키 | `SameSite=None` | 불가피하나, PKCE 적용 검토 |
| 관리자 쿠키 | `SameSite=Lax` | 가능하다면 `Strict`로 강화 검토 |
| CORS 허용 헤더 | `*` (전체) | 필요한 헤더만 명시적으로 허용 권고 |
| 토큰 갱신 시 Redis 무효화 | 확인 필요 | 이전 토큰이 만료 전 재사용 가능한지 점검 |

---

## 7. 결론

**CSRF 토큰 도입은 불필요합니다.**

현재 아키텍처는:
- Spring Security 미사용으로 CSRF 필터 자체가 없고
- 모든 API가 REST 방식이며 HTML Form 제출이 없고
- SameSite=Strict 쿠키로 이미 CSRF 공격을 실질적으로 차단하고 있습니다.

만약 향후 Spring Security를 도입한다면, JWT나 현재처럼 커스텀 토큰 기반 Stateless 인증을 사용하는 경우에는 `http.csrf(AbstractHttpConfigurer::disable)`로 명시적으로 비활성화하는 것이 올바른 설정입니다.
