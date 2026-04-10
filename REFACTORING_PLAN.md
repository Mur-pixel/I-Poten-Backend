# I-Poten Backend - 리팩터링 & 보안 강화 계획

> 작성일: 2026-03-19

---

## 현황 요약 (문제의 출발점)

현재 28개 컨트롤러, 76개 이상의 메서드에서 아래 패턴이 반복됨:

```java
// 모든 컨트롤러에 이 코드가 흩어져 있음
@PostMapping("/something")
public ResponseEntity<?> doSomething(
        @CookieValue(name = "userToken", required = false) String userToken) {
    Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
    if (accountId == null) {
        return ResponseEntity.status(401).build();  // 컨트롤러마다 다르게 처리
    }
    // ...
}
```

그리고 여러 컨트롤러에 `resolveAccountId()` 헬퍼가 각각 독립적으로 복사되어 있음.

---

## Part 1. 인터셉터 + 커스텀 어노테이션 설계

### 1-1. 전체 구조

```
요청
 │
 ▼
[AuthInterceptor]              ← userToken 쿠키 추출 & Redis 검증
 │   ├─ @PublicApi 붙은 메서드  → 검증 없이 통과
 │   ├─ 토큰 없음/만료          → 401 즉시 반환
 │   └─ 검증 성공              → request.setAttribute("accountId", accountId)
 │
 ▼
[컨트롤러 메서드]
 │   @AuthRequired 붙은 메서드 → @CurrentAccount로 accountId 바로 주입
 │
 ▼
[서비스 레이어]                ← accountId만 받아서 처리, 토큰 모름
```

### 1-2. 커스텀 어노테이션 2개

**① `@PublicApi` - 인증 없이 접근 가능한 엔드포인트 표시**

```java
// annotation/PublicApi.java
package com.cygnus.ipoten.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicApi {
}
```

**② `@CurrentAccount` - 인증된 accountId를 파라미터로 주입**

```java
// annotation/CurrentAccount.java
package com.cygnus.ipoten.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentAccount {
}
```

### 1-3. AuthInterceptor 구현

```java
// interceptor/AuthInterceptor.java
package com.cygnus.ipoten.common.interceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String ACCOUNT_ID_ATTR = "accountId";
    private static final String USER_TOKEN_COOKIE = "userToken";

    private final RedisCacheService redisCacheService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        // @PublicApi가 클래스 또는 메서드에 붙어있으면 통과
        boolean isPublic = method.hasMethodAnnotation(PublicApi.class)
                || method.getBeanType().isAnnotationPresent(PublicApi.class);
        if (isPublic) {
            return true;
        }

        // 쿠키에서 userToken 추출
        String userToken = extractUserToken(request);
        if (userToken == null || userToken.isBlank()) {
            sendUnauthorized(response);
            return false;
        }

        // Redis에서 accountId 조회
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        if (accountId == null) {
            sendUnauthorized(response);
            return false;
        }

        // 이후 처리에서 꺼내 쓸 수 있도록 저장
        request.setAttribute(ACCOUNT_ID_ATTR, accountId);
        return true;
    }

    private String extractUserToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(c -> USER_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"인증이 필요합니다.\"}");
    }
}
```

### 1-4. `@CurrentAccount` 파라미터 리졸버

```java
// resolver/CurrentAccountArgumentResolver.java
package com.cygnus.ipoten.common.resolver;

@Component
public class CurrentAccountArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAccount.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return request.getAttribute("accountId");  // 인터셉터가 저장한 값
    }
}
```

### 1-5. WebConfig에 등록

```java
// config/WebConfig.java
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CurrentAccountArgumentResolver currentAccountArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")          // 인증 필요한 경로
                .excludePathPatterns(               // 인터셉터 자체 제외 (PublicApi로 처리해도 됨)
                        "/authentication/**",
                        "/kakao-authentication/**",
                        "/google-authentication/**",
                        "/naver-authentication/**",
                        "/github-authentication/**",
                        "/apple-authentication/**",
                        "/meta-authentication/**"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAccountArgumentResolver);
    }
}
```

### 1-6. 컨트롤러 Before / After 비교

**Before (현재)**
```java
// 28개 컨트롤러마다 이 코드 반복
@GetMapping("/profile")
public ResponseEntity<ProfileResponse> getProfile(
        @CookieValue(name = "userToken", required = false) String userToken) {

    Long accountId = redisCacheService.getValueByKey(userToken, Long.class); // ← 반복
    if (accountId == null) {                                                  // ← 반복
        return ResponseEntity.status(401).build();                            // ← 반복
    }
    return ResponseEntity.ok(profileService.getProfile(accountId));
}
```

**After (리팩터링 후)**
```java
@GetMapping("/profile")
public ResponseEntity<ProfileResponse> getProfile(
        @CurrentAccount Long accountId) {   // ← 인터셉터가 검증 완료 후 주입

    return ResponseEntity.ok(profileService.getProfile(accountId));
}
```

공개 API는 어노테이션 하나로 처리:
```java
@PublicApi
@GetMapping("/health")
public ResponseEntity<String> health() {
    return ResponseEntity.ok("ok");
}
```

### 1-7. 로그아웃 - 쿠키 삭제 유틸

현재 `AuthenticationController`에서 `Cookie` 객체를 직접 만들어 삭제 중인데,
`HttpOnly` 쿠키는 `response.addCookie()`로 삭제 시 `HttpOnly` 속성이 유지되지 않아
브라우저에서 실제로 삭제되지 않을 수 있음. 아래 방식으로 통일:

```java
// 쿠키 삭제 - Set-Cookie 헤더 직접 설정
public static void clearUserTokenCookie(HttpServletResponse response) {
    response.addHeader("Set-Cookie",
        "userToken=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");
}
```

---

## Part 2. 프로덕션 보안 강화 필수 항목

### [P1] 즉시 적용 - 위험도 높음

#### 2-1. 로그아웃 쿠키 삭제 버그 수정

현재 `AuthenticationController`의 로그아웃:
```java
// 현재 코드 - HttpOnly 속성이 없어서 브라우저가 쿠키를 실제로 삭제하지 않을 수 있음
Cookie cookie = new Cookie("userToken", null);
cookie.setMaxAge(0);
cookie.setPath("/");
response.addCookie(cookie);
```

수정:
```java
// HttpOnly + Secure + SameSite 전부 포함해서 삭제
response.addHeader("Set-Cookie",
    "userToken=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");
```

#### 2-2. 쿠키 Set-Cookie 문자열 중앙화

현재 6개 파일에 `String.format("userToken=%s; Max-Age=%d; ...")` 이 반복.
하나라도 다르게 쓰이면 보안 속성 누락 위험.

```java
// util/CookieUtil.java
public class CookieUtil {
    private static final int USER_TOKEN_MAX_AGE = 6 * 60 * 60;

    public static String buildUserTokenCookie(String token) {
        return String.format(
            "userToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict",
            token, USER_TOKEN_MAX_AGE
        );
    }

    public static String clearUserTokenCookie() {
        return "userToken=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict";
    }
}
```

#### 2-3. 인증 엔드포인트 Rate Limiting

현재 `/api/authentication/`, 각 OAuth 콜백, 관리자 로그인 등에 요청 횟수 제한 없음.
Bucket4j + Redis로 슬라이딩 윈도우 적용 권고:

```groovy
// build.gradle
implementation 'com.bucket4j:bucket4j-core:8.10.1'
implementation 'com.bucket4j:bucket4j-redis:8.10.1'
```

보호 대상 엔드포인트:
- `POST /administrator/authentication/code_login`
- `GET /authentication/*/login?code=` (OAuth 콜백)
- `GET /api/authentication/token/verification`

#### 2-4. 관리자 토큰 SameSite 강화

현재 `temporaryAdminToken`은 `SameSite=Lax`. 관리자 패널은 외부 링크를 통한 진입이 필요 없으므로 `Strict`로 변경:

```java
// 변경 전
"temporaryAdminToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax"

// 변경 후
"temporaryAdminToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict"
```

`Secure` 플래그도 현재 빠져 있음 - 추가 필요.

#### 2-5. CORS allowedHeaders 명시화

현재:
```java
.allowedHeaders("*")  // 모든 헤더 허용
```

변경:
```java
.allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "Accept")
```

필요한 헤더만 명시. `*`는 공격자가 임의 헤더를 포함할 수 있는 여지를 줌.

---

### [P2] 단기 적용 권고

#### 2-6. Redis 장애 시 Fallback 없음

현재 Redis가 다운되면 모든 인증이 `null` 반환 → 전체 401.
Redis 예외를 잡아 503 명확히 반환하고 알람 연동 권고:

```java
try {
    Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
} catch (RedisConnectionFailureException e) {
    // Redis 장애 - 503 반환 및 알람
    log.error("Redis connection failed during authentication", e);
    response.setStatus(503);
    return false;
}
```

#### 2-7. accountId null 미검증 진입 가능 케이스

일부 컨트롤러(예: `AccountProfileController.getProfile()`)는 `accountId`가 null임에도
직접 서비스를 호출해 `NullPointerException` 또는 DB 조회 오류가 발생.
인터셉터 도입으로 근본 해결되지만, 그 전까지는 방어 코드 추가 필요.

#### 2-8. 토큰 로깅 금지

서비스/컨트롤러 로그에 `userToken` 값이 출력되지 않도록 확인.
MDC에 토큰 대신 accountId만 포함:

```java
MDC.put("accountId", String.valueOf(accountId));
// userToken 자체는 로그에 절대 출력 금지
```

#### 2-9. Temporary 토큰 명시적 처리

현재 `AuthenticationController.verifyToken()`에서만 `Temporary_` prefix 체크.
다른 API에서 temporaryToken으로 접근 가능한지 전수 확인 필요.
인터셉터 도입 시 아래 조건 추가:

```java
if (userToken.startsWith("Temporary_")) {
    sendForbidden(response, "임시 토큰으로는 접근할 수 없습니다.");
    return false;
}
```

---

### [P3] 중장기 검토

#### 2-10. GitHub OAuth SameSite=None 위험 최소화

GitHub OAuth 리다이렉트 특성상 `SameSite=None`이 불가피하나,
콜백 이후 즉시 `SameSite=Strict` 토큰으로 교체하는 흐름인지 확인.
현재 코드에서 GitHub 콜백이 `SameSite=None` 토큰을 직접 발급하고 있다면 수정 필요.

#### 2-11. Refresh Token 재사용 공격 방지

`mobile_auth`의 Refresh Token이 갱신 시 이전 토큰을 즉시 무효화하는지 (Token Rotation + Reuse Detection) 확인.

---

## Part 3. 대대적인 리팩터링 항목

### 3-1. 인증 흐름 중앙화 (최우선)

**현황:** 각 OAuth 컨트롤러(6개)가 독립적으로 토큰 생성 → 쿠키 설정
**목표:** 인증 성공 후 공통 흐름으로 합치기

```java
// 현재: 6개 OAuth 컨트롤러 각각 이 코드를 가짐
String userToken = authService.createUserTokenWithAccessToken(accountId, accessToken);
String cookieHeader = String.format("userToken=%s; ...", userToken, 6 * 60 * 60);
response.addHeader("Set-Cookie", cookieHeader);

// 개선: AuthFacade 또는 LoginSuccessHandler로 통합
@Component
public class LoginSuccessHandler {
    public void handle(HttpServletResponse response, Long accountId, String accessToken) {
        String token = authService.createUserTokenWithAccessToken(accountId, accessToken);
        response.addHeader("Set-Cookie", CookieUtil.buildUserTokenCookie(token));
    }
}
```

### 3-2. 컨트롤러 패키지 구조 재정리

**현황:** 최상위 패키지에 15개 이상의 `*_authentication` 패키지가 평행으로 존재

```
com.cygnus.ipoten/
├── google_authentication/
├── kakao_authentication/
├── naver_authentication/
├── github_authentication/
├── apple_authentication/
├── meta_authentication/
├── interview/
├── quiz_session/
...
```

**제안 구조:**
```
com.cygnus.ipoten/
├── auth/
│   ├── oauth/
│   │   ├── google/
│   │   ├── kakao/
│   │   └── ...
│   ├── mobile/
│   └── core/       ← AuthenticationService, RedisCacheService
├── account/
├── interview/
├── quiz/
├── wordbook/
├── common/
│   ├── annotation/
│   ├── interceptor/
│   ├── resolver/
│   └── util/
└── config/
```

### 3-3. `resolveAccountId()` 헬퍼 제거

**현황:** 최소 2개 컨트롤러에 동일한 private 메서드가 각각 존재. 인터셉터 도입으로 완전히 제거 가능.

### 3-4. 일관되지 않은 401 응답 형식 통일

**현황:**
```java
// 컨트롤러 A
return ResponseEntity.status(401).build();  // body 없음

// 컨트롤러 B
throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");

// 컨트롤러 C
return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new TokenAuthenticationExpiredResponseForm(false));
```

**개선:** `@ControllerAdvice` + 공통 에러 응답 클래스 도입

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException e) {
        return ResponseEntity.status(401)
                .body(new ErrorResponse("UNAUTHORIZED", e.getMessage()));
    }
}
```

### 3-5. RedisCacheService 제네릭 한계 개선

**현황:**
```java
// 타입 안전하지 않음 - String/Integer/Long만 지원하는 if-else 분기
public <T> T getValueByKey(String key, Class<T> clazz) {
    if(clazz == String.class) return clazz.cast(value);
    if(clazz == Long.class) return clazz.cast(Long.valueOf(value));
    // Integer만 더 있고, 나머지 타입은 null 반환
}
```

**개선:** JSON 직렬화 통합 또는 Jackson ObjectMapper 위임

```java
public <T> T getValueByKey(String key, Class<T> clazz) {
    String value = redisTemplate.opsForValue().get(key);
    if (value == null) return null;
    return objectMapper.readValue(value, clazz);
}
```

### 3-6. 쿠키 생성 로직 중앙화 (CookieUtil)

중복 포인트:
- `KakaoAuthenticationController`
- `GoogleAuthenticationController`
- `NaverAuthenticationController`
- `GithubAuthenticationController`
- `AccountController`
- `AdministratorLoginController`

→ `CookieUtil.buildUserTokenCookie(token)` 한 곳으로 수렴

---

## 실행 우선순위 요약

| 우선순위 | 항목 | 난이도 | 효과 |
|---------|------|--------|------|
| P1 (즉시) | 로그아웃 쿠키 삭제 버그 수정 | 낮음 | 보안 |
| P1 (즉시) | CookieUtil 중앙화 | 낮음 | 유지보수 |
| P1 (즉시) | 관리자 토큰 Secure + SameSite=Strict | 낮음 | 보안 |
| P1 (즉시) | CORS allowedHeaders 명시화 | 낮음 | 보안 |
| P1 (단기) | 인터셉터 + @CurrentAccount 도입 | 중간 | 구조 개선 |
| P1 (단기) | Temporary 토큰 인터셉터에서 차단 | 낮음 | 보안 |
| P2 (단기) | Rate Limiting (인증 엔드포인트) | 중간 | 보안 |
| P2 (단기) | 공통 에러 응답 (@ControllerAdvice) | 중간 | 유지보수 |
| P2 (단기) | Redis 장애 Fallback | 중간 | 안정성 |
| P3 (중기) | 패키지 구조 재정리 | 높음 | 구조 개선 |
| P3 (중기) | LoginSuccessHandler 통합 | 중간 | 구조 개선 |
| P3 (중기) | RedisCacheService ObjectMapper 개선 | 중간 | 유지보수 |
