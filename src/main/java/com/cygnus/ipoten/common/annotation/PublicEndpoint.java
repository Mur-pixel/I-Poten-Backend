package com.cygnus.ipoten.common.annotation;

import java.lang.annotation.*;

/**
 * 인증 없이 접근 가능한 엔드포인트를 표시합니다.
 * AuthenticationInterceptor 가 이 어노테이션을 확인하고 토큰 검증을 건너뜁니다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicEndpoint {
}
