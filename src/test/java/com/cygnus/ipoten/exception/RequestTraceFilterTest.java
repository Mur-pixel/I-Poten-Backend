package com.cygnus.ipoten.exception;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("RequestTraceFilter 테스트")
class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @Test
    @DisplayName("헤더 없을 때 traceId 가 자동 생성되어 응답 헤더로 내려간다")
    void generatesTraceIdWhenAbsent() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        String traceId = res.getHeader(RequestTraceFilter.HEADER_NAME);
        assertThat(traceId).isNotBlank();
        assertThat(traceId).hasSize(32); // UUID without dashes
        verify(chain).doFilter(req, res);
    }

    @Test
    @DisplayName("요청 헤더의 traceId 를 그대로 사용한다 (분산 추적 호환)")
    void preservesIncomingTraceId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(RequestTraceFilter.HEADER_NAME, "upstream-trace-42");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertThat(res.getHeader(RequestTraceFilter.HEADER_NAME)).isEqualTo("upstream-trace-42");
    }

    @Test
    @DisplayName("필터 종료 후 MDC 가 정리된다 (스레드 풀 누수 방지)")
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertThat(MDC.get(RequestTraceFilter.MDC_KEY)).isNull();
    }
}
