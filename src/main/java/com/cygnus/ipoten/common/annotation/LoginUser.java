package com.cygnus.ipoten.common.annotation;

import java.lang.annotation.*;

/**
 * AuthenticationInterceptor 가 검증한 인증된 사용자의 accountId 를 파라미터로 주입합니다.
 *
 * <pre>
 * {@code
 * public ResponseEntity<?> example(@LoginUser Long accountId) { ... }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoginUser {
}
