package com.cygnus.ipoten.exception;

import com.cygnus.ipoten.account.exception.NotLoggedInException;
import com.cygnus.ipoten.account.exception.UserNotFoundException;
import com.cygnus.ipoten.authentication.social.SocialLoginErrorResponse;
import com.cygnus.ipoten.authentication.social.SocialLoginException;
import com.cygnus.ipoten.google_authentication.exception.GoogleAccessTokenException;
import com.cygnus.ipoten.google_authentication.exception.GoogleGetUserInfoException;
import com.cygnus.ipoten.term.exception.TermNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 전역 예외 핸들러.
 *
 * <p>모든 예외를 표준 {@link ErrorResponse} 로 변환한다.
 * 도메인 예외 → {@link BusinessException} + {@link ErrorCode}
 * 검증 실패 → {@code MethodArgumentNotValidException} 등
 * 그 외 시스템 예외 → 5xx, 로그 + 사용자에게는 일반 메시지만 노출.
 *
 * <p>레거시 예외(Term/User/SocialLogin 등)도 호환을 위해 그대로 처리하지만,
 * 신규 코드에서는 {@link BusinessException} 사용을 권장한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 비즈니스 예외 ─────────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorCode ec = ex.getErrorCode();
        log.warn("[business] code={} status={} path={} message={}",
                ec.getCode(), ec.getStatus(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(ec.getStatus())
                .body(ErrorResponse.of(ec, ex.getMessage(), req.getRequestURI(), traceId()));
    }

    // ── 입력 검증 실패 (@Valid @RequestBody) ──────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        List<ErrorResponse.FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toDetail)
                .toList();
        log.warn("[validation] path={} fieldErrors={}", req.getRequestURI(), details);
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getStatus())
                .body(ErrorResponse.withFieldErrors(
                        ErrorCode.COMMON_INVALID_INPUT, req.getRequestURI(), traceId(), details));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldErrorDetail> details = ex.getFieldErrors().stream()
                .map(GlobalExceptionHandler::toDetail)
                .toList();
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getStatus())
                .body(ErrorResponse.withFieldErrors(
                        ErrorCode.COMMON_INVALID_INPUT, req.getRequestURI(), traceId(), details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex,
                                                          HttpServletRequest req) {
        List<ErrorResponse.FieldErrorDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldErrorDetail(
                        v.getPropertyPath() != null ? v.getPropertyPath().toString() : "param",
                        v.getMessage()))
                .toList();
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getStatus())
                .body(ErrorResponse.withFieldErrors(
                        ErrorCode.COMMON_INVALID_INPUT, req.getRequestURI(), traceId(), details));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedBody(Exception ex, HttpServletRequest req) {
        log.warn("[bad-input] path={} cause={}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT,
                        "요청 본문이 올바르지 않습니다.", req.getRequestURI(), traceId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest req) {
        log.warn("[illegal-argument] path={} message={}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT, ex.getMessage(),
                        req.getRequestURI(), traceId()));
    }

    // ── 라우팅 ────────────────────────────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                          HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.COMMON_RESOURCE_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_RESOURCE_NOT_FOUND,
                        req.getRequestURI(), traceId()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.COMMON_METHOD_NOT_ALLOWED.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_METHOD_NOT_ALLOWED, ex.getMessage(),
                        req.getRequestURI(), traceId()));
    }

    // ── 레거시 ResponseStatusException ────────────────────────
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex,
                                                              HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ErrorCode mapped  = mapStatus(status);
        log.warn("[rse] status={} reason={} path={}", status, ex.getReason(), req.getRequestURI());
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(mapped, ex.getReason(), req.getRequestURI(), traceId()));
    }

    // ── 도메인 레거시 예외 (점진 이관 예정) ─────────────────
    @ExceptionHandler(TermNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTermNotFound(TermNotFoundException ex,
                                                            HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.TERM_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.TERM_NOT_FOUND, ex.getMessage(),
                        req.getRequestURI(), traceId()));
    }

    @ExceptionHandler(NotLoggedInException.class)
    public ResponseEntity<ErrorResponse> handleNotLoggedIn(NotLoggedInException ex,
                                                           HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.AUTH_NOT_LOGGED_IN.getStatus())
                .body(ErrorResponse.of(ErrorCode.AUTH_NOT_LOGGED_IN, ex.getMessage(),
                        req.getRequestURI(), traceId()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex,
                                                            HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.ACCOUNT_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.ACCOUNT_NOT_FOUND, ex.getMessage(),
                        req.getRequestURI(), traceId()));
    }

    @ExceptionHandler(GoogleAccessTokenException.class)
    public ResponseEntity<ErrorResponse> handleGoogleAccessToken(GoogleAccessTokenException ex,
                                                                 HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.EXTERNAL_GOOGLE_TOKEN_FAILED.getStatus())
                .body(ErrorResponse.of(ErrorCode.EXTERNAL_GOOGLE_TOKEN_FAILED, ex.getMessage(),
                        req.getRequestURI(), traceId()));
    }

    @ExceptionHandler(GoogleGetUserInfoException.class)
    public ResponseEntity<ErrorResponse> handleGoogleUserInfo(GoogleGetUserInfoException ex,
                                                              HttpServletRequest req) {
        return ResponseEntity
                .status(ErrorCode.EXTERNAL_GOOGLE_USERINFO_FAILED.getStatus())
                .body(ErrorResponse.of(ErrorCode.EXTERNAL_GOOGLE_USERINFO_FAILED, ex.getMessage(),
                        req.getRequestURI(), traceId()));
    }

    /**
     * 소셜 로그인은 OAuth provider 호환을 위해 기존 응답 포맷을 유지한다.
     * 모바일/웹 클라이언트가 이미 본 포맷에 의존 중이므로 별도 키로 응답한다.
     */
    @ExceptionHandler(SocialLoginException.class)
    public ResponseEntity<SocialLoginErrorResponse> handleSocialLogin(SocialLoginException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.toResponse());
    }

    // ── 최종 fallback ─────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        // 사용자에게 내부 예외 상세를 노출하지 않는다. traceId 로 서버 로그와 매칭.
        log.error("[unexpected] path={} traceId={}", req.getRequestURI(), traceId(), ex);
        return ResponseEntity
                .status(ErrorCode.COMMON_INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.COMMON_INTERNAL_ERROR,
                        req.getRequestURI(), traceId()));
    }

    // ── helpers ───────────────────────────────────────────────
    private static String traceId() {
        String t = MDC.get(RequestTraceFilter.MDC_KEY);
        return t != null ? t : "-";
    }

    private static ErrorResponse.FieldErrorDetail toDetail(FieldError fe) {
        return new ErrorResponse.FieldErrorDetail(
                fe.getField(),
                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값입니다.");
    }

    private static ErrorCode mapStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST       -> ErrorCode.COMMON_INVALID_INPUT;
            case UNAUTHORIZED      -> ErrorCode.AUTH_NOT_LOGGED_IN;
            case FORBIDDEN         -> ErrorCode.AUTH_FORBIDDEN;
            case NOT_FOUND         -> ErrorCode.COMMON_RESOURCE_NOT_FOUND;
            case METHOD_NOT_ALLOWED-> ErrorCode.COMMON_METHOD_NOT_ALLOWED;
            case CONFLICT          -> ErrorCode.COMMON_CONFLICT;
            case PAYMENT_REQUIRED  -> ErrorCode.COMMON_PAYMENT_REQUIRED;
            default                -> ErrorCode.COMMON_INTERNAL_ERROR;
        };
    }
}
