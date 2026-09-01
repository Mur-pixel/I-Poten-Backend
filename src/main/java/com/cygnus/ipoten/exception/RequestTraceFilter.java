package com.cygnus.ipoten.exception;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 요청에 traceId 를 부여하여 MDC 에 저장하고,
 * 응답 헤더({@code X-Trace-Id}) 로 내려보낸다.
 *
 * <p>로그 패턴(logback 등)에 {@code %X{traceId}} 를 추가하면
 * 동일 요청에서 발생한 모든 로그를 한 줄로 추적할 수 있다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTraceFilter extends OncePerRequestFilter {

    public  static final String HEADER_NAME = "X-Trace-Id";
    public  static final String MDC_KEY     = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String traceId = request.getHeader(HEADER_NAME);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        try {
            MDC.put(MDC_KEY, traceId);
            response.setHeader(HEADER_NAME, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
