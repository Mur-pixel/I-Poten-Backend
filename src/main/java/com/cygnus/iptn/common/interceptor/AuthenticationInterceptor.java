package com.cygnus.iptn.common.interceptor;

import com.cygnus.iptn.common.annotation.PublicEndpoint;
import com.cygnus.iptn.common.util.CookieUtil;
import com.cygnus.iptn.common.util.InternalApiKeyValidator;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * userToken HttpOnly 쿠키를 한 곳에서 추출·검증하는 인터셉터.
 *
 * <p>흐름:</p>
 * <ol>
 *   <li>@PublicEndpoint → 검증 없이 통과</li>
 *   <li>X-Internal-Key 헤더가 유효하면 → 내부 서비스 호출로 통과</li>
 *   <li>userToken 쿠키 없음 / Temporary_ 토큰 → 401 즉시 반환</li>
 *   <li>Redis 에서 accountId 조회 실패(만료/무효) → 401 즉시 반환</li>
 *   <li>검증 성공 → request 에 accountId, userToken 저장 후 통과</li>
 * </ol>
 *
 * <p>request attribute 키:</p>
 * <ul>
 *   <li>{@code _accountId} : Long — {@code @LoginUser} 리졸버가 읽음</li>
 *   <li>{@code _userToken} : String — {@code @LoginToken} 리졸버가 읽음</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {

    public static final String ACCOUNT_ID_ATTR = "_accountId";
    public static final String USER_TOKEN_ATTR  = "_userToken";

    private final RedisCacheService redisCacheService;
    private final InternalApiKeyValidator internalApiKeyValidator;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        // @PublicEndpoint 가 메서드 또는 클래스에 붙어있으면 통과
        if (isPublicEndpoint(method)) {
            return true;
        }

        // X-Internal-Key 헤더가 유효하면 내부 서비스 호출로 통과
        String internalKey = request.getHeader("X-Internal-Key");
        if (internalKey != null && internalApiKeyValidator.isValid(internalKey)) {
            return true;
        }

        String userToken = CookieUtil.extract(request, CookieUtil.USER_TOKEN_COOKIE);

        if (userToken == null || userToken.isBlank() || userToken.startsWith(CookieUtil.TEMPORARY_TOKEN_PREFIX)) {
            sendUnauthorized(response);
            return false;
        }

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        if (accountId == null) {
            sendUnauthorized(response);
            return false;
        }

        request.setAttribute(ACCOUNT_ID_ATTR, accountId);
        request.setAttribute(USER_TOKEN_ATTR, userToken);
        return true;
    }

    private boolean isPublicEndpoint(HandlerMethod method) {
        return method.hasMethodAnnotation(PublicEndpoint.class)
                || method.getBeanType().isAnnotationPresent(PublicEndpoint.class);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"인증이 필요합니다.\"}");
    }
}
