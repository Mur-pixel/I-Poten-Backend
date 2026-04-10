package com.cygnus.ipoten.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * userToken HttpOnly 쿠키 관련 공통 유틸리티.
 *
 * <p>쿠키 이름·prefix·TTL 상수와 추출/설정/삭제 메서드를 한 곳에서 관리합니다.</p>
 */
public final class CookieUtil {

    public static final String USER_TOKEN_COOKIE       = "userToken";
    public static final String TEMPORARY_TOKEN_PREFIX  = "Temporary_";
    public static final int    USER_TOKEN_MAX_AGE       = 6 * 60 * 60; // 6시간

    private CookieUtil() {}

    /** 요청에서 특정 이름의 쿠키 값을 추출합니다. 없으면 null 반환. */
    public static String extract(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    /**
     * userToken 쿠키를 설정합니다.
     * HttpOnly, Secure, SameSite=Strict, TTL 6시간.
     */
    public static void addUserToken(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", String.format(
                "userToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict",
                token, USER_TOKEN_MAX_AGE
        ));
    }

    /**
     * userToken 쿠키를 삭제합니다.
     * Max-Age=0 으로 브라우저 쿠키 즉시 만료.
     */
    public static void clearUserToken(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
                "userToken=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict");
    }
}
