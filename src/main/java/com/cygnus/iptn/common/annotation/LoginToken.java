package com.cygnus.iptn.common.annotation;

import java.lang.annotation.*;

/**
 * AuthenticationInterceptor 가 검증한 userToken 원본을 파라미터로 주입합니다.
 * 서비스 레이어에 raw token 을 직접 전달해야 하는 소수의 케이스에만 사용합니다.
 *
 * <pre>
 * {@code
 * public ResponseEntity<?> example(@LoginToken String userToken) { ... }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoginToken {
}
