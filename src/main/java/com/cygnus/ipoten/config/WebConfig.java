package com.cygnus.ipoten.config;

import com.cygnus.ipoten.common.interceptor.AuthenticationInterceptor;
import com.cygnus.ipoten.common.resolver.LoginTokenArgumentResolver;
import com.cygnus.ipoten.common.resolver.LoginUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 인터셉터 및 ArgumentResolver 등록.
 *
 * <p>인터셉터 적용 경로:</p>
 * <ul>
 *   <li>{@code /api/**} — 일반 API</li>
 *   <li>{@code /account-profile/**} — 프로필 API</li>
 *   <li>{@code /credit/**} — 크레딧 API</li>
 * </ul>
 *
 * <p>인터셉터 제외 경로 (OAuth 전용 경로는 자체 인증 사용):</p>
 * <ul>
 *   <li>{@code /authentication/**}</li>
 *   <li>{@code /kakao-authentication/**}</li>
 *   <li>{@code /google-authentication/**}</li>
 *   <li>{@code /naver-authentication/**}</li>
 *   <li>{@code /github-authentication/**}</li>
 *   <li>{@code /apple-authentication/**}</li>
 *   <li>{@code /meta-authentication/**}</li>
 * </ul>
 *
 * <p>위 경로 내에서도 {@code @PublicEndpoint} 가 붙은 메서드는 검증을 건너뜁니다.</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;
    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final LoginTokenArgumentResolver loginTokenArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor)
                .addPathPatterns(
                        "/api/**",
                        "/account-profile/**",
                        "/credit/**"
                )
                .excludePathPatterns(
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
        resolvers.add(loginUserArgumentResolver);
        resolvers.add(loginTokenArgumentResolver);
    }
}
