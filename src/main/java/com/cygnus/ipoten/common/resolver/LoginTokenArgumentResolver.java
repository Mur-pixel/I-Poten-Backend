package com.cygnus.ipoten.common.resolver;

import com.cygnus.ipoten.common.annotation.LoginToken;
import com.cygnus.ipoten.common.interceptor.AuthenticationInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @LoginToken String userToken} 파라미터를 request attribute 에서 주입합니다.
 * AuthenticationInterceptor 가 사전에 검증한 raw userToken 을 전달합니다.
 * 서비스 레이어에 토큰을 직접 전달해야 하는 경우에만 사용합니다.
 */
@Component
public class LoginTokenArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginToken.class)
                && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        return request.getAttribute(AuthenticationInterceptor.USER_TOKEN_ATTR);
    }
}
