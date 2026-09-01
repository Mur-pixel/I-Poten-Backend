package com.cygnus.ipoten.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest mockRequest(String uri) {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

    @Nested
    @DisplayName("BusinessException 처리")
    class BusinessExceptionHandling {

        @Test
        @DisplayName("에러 코드의 status / code / message 가 응답에 그대로 매핑된다")
        void mapsErrorCodeFields() {
            BusinessException ex = new BusinessException(ErrorCode.CREDIT_INSUFFICIENT);
            ResponseEntity<ErrorResponse> res =
                    handler.handleBusiness(ex, mockRequest("/api/credit/pay"));

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().code()).isEqualTo("CREDIT_001");
            assertThat(res.getBody().message()).isEqualTo("보유 크레딧이 부족합니다.");
            assertThat(res.getBody().path()).isEqualTo("/api/credit/pay");
            assertThat(res.getBody().status()).isEqualTo(402);
            assertThat(res.getBody().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("override 메시지가 있으면 기본 메시지를 대체한다")
        void overrideMessage() {
            BusinessException ex = new BusinessException(
                    ErrorCode.WORDBOOK_NOT_FOUND, "id=42 단어장 없음");
            ResponseEntity<ErrorResponse> res =
                    handler.handleBusiness(ex, mockRequest("/api/me/folders/42"));

            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().message()).isEqualTo("id=42 단어장 없음");
            assertThat(res.getBody().code()).isEqualTo("WORDBOOK_001");
        }
    }

    @Nested
    @DisplayName("ResponseStatusException 처리")
    class RseHandling {

        @Test
        @DisplayName("404 RSE 는 COMMON_RESOURCE_NOT_FOUND 로 매핑된다")
        void notFound() {
            ResponseStatusException ex = new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "no such");
            ResponseEntity<ErrorResponse> res =
                    handler.handleResponseStatus(ex, mockRequest("/api/foo"));

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().code()).isEqualTo("COMMON_002");
            assertThat(res.getBody().message()).isEqualTo("no such");
        }

        @Test
        @DisplayName("401 RSE 는 AUTH_NOT_LOGGED_IN 로 매핑된다")
        void unauthorized() {
            ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            ResponseEntity<ErrorResponse> res =
                    handler.handleResponseStatus(ex, mockRequest("/api/me"));

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().code()).isEqualTo("AUTH_001");
        }
    }

    @Nested
    @DisplayName("기타 예외 처리")
    class MiscHandling {

        @Test
        @DisplayName("IllegalArgumentException → 400 + COMMON_INVALID_INPUT")
        void illegalArgument() {
            ResponseEntity<ErrorResponse> res = handler.handleIllegalArgument(
                    new IllegalArgumentException("bad value"), mockRequest("/api/x"));

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().code()).isEqualTo("COMMON_001");
            assertThat(res.getBody().message()).isEqualTo("bad value");
        }

        @Test
        @DisplayName("최종 fallback 은 500 + 사용자에게 내부 메시지를 노출하지 않는다")
        void unexpectedDoesNotLeakInternals() {
            ResponseEntity<ErrorResponse> res = handler.handleUnexpected(
                    new NullPointerException("internal stacktrace"),
                    mockRequest("/api/x"));

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().code()).isEqualTo("COMMON_999");
            assertThat(res.getBody().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
            assertThat(res.getBody().message()).doesNotContain("internal stacktrace");
        }

        @Test
        @DisplayName("ConstraintViolationException 은 fieldErrors 로 변환된다")
        void constraintViolation() {
            ConstraintViolation<?> v = Mockito.mock(ConstraintViolation.class);
            jakarta.validation.Path path = Mockito.mock(jakarta.validation.Path.class);
            Mockito.when(path.toString()).thenReturn("createWordbook.name");
            Mockito.when(v.getPropertyPath()).thenReturn(path);
            Mockito.when(v.getMessage()).thenReturn("공백일 수 없습니다.");

            ConstraintViolationException ex = new ConstraintViolationException(Set.of(v));
            ResponseEntity<ErrorResponse> res =
                    handler.handleConstraint(ex, mockRequest("/api/me/folders"));

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().errors()).hasSize(1);
            assertThat(res.getBody().errors().get(0).field()).isEqualTo("createWordbook.name");
            assertThat(res.getBody().errors().get(0).reason()).isEqualTo("공백일 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("MDC traceId 전달")
    class TraceId {

        @Test
        @DisplayName("MDC 의 traceId 가 응답 traceId 로 전달된다")
        void traceIdFlowsThrough() {
            MDC.put(RequestTraceFilter.MDC_KEY, "abc123");
            try {
                ResponseEntity<ErrorResponse> res = handler.handleBusiness(
                        new BusinessException(ErrorCode.WORDBOOK_NOT_FOUND),
                        mockRequest("/api/me/folders/1"));
                assertThat(res.getBody()).isNotNull();
                assertThat(res.getBody().traceId()).isEqualTo("abc123");
            } finally {
                MDC.remove(RequestTraceFilter.MDC_KEY);
            }
        }

        @Test
        @DisplayName("MDC 미설정 시 traceId 는 '-' 로 응답된다")
        void traceIdDefault() {
            MDC.remove(RequestTraceFilter.MDC_KEY);
            ResponseEntity<ErrorResponse> res = handler.handleBusiness(
                    new BusinessException(ErrorCode.WORDBOOK_NOT_FOUND),
                    mockRequest("/api/me/folders/1"));
            assertThat(res.getBody()).isNotNull();
            assertThat(res.getBody().traceId()).isEqualTo("-");
        }
    }
}
