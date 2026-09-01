package com.cygnus.ipoten.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 표준 에러 응답.
 *
 * <pre>
 * {
 *   "code":      "WORDBOOK_001",
 *   "message":   "단어장을 찾을 수 없습니다.",
 *   "status":    404,
 *   "path":      "/api/me/folders/123",
 *   "traceId":   "8f4d92b1...",
 *   "timestamp": "2026-04-26T12:34:56+09:00",
 *   "errors": [
 *     {"field":"wordbookName","reason":"공백일 수 없습니다."}
 *   ]
 * }
 * </pre>
 *
 * <p>{@code errors} 는 {@link jakarta.validation.Valid} 검증 실패 시에만 채워지며,
 * 일반 비즈니스 예외에서는 생략된다 (Jackson NON_NULL).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String                 code,
        String                 message,
        int                    status,
        String                 path,
        String                 traceId,
        OffsetDateTime         timestamp,
        List<FieldErrorDetail> errors
) {

    public static ErrorResponse of(ErrorCode errorCode, String path, String traceId) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                errorCode.getStatus().value(),
                path,
                traceId,
                OffsetDateTime.now(),
                null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String overrideMessage,
                                   String path, String traceId) {
        return new ErrorResponse(
                errorCode.getCode(),
                overrideMessage != null ? overrideMessage : errorCode.getDefaultMessage(),
                errorCode.getStatus().value(),
                path,
                traceId,
                OffsetDateTime.now(),
                null
        );
    }

    public static ErrorResponse withFieldErrors(ErrorCode errorCode, String path, String traceId,
                                                List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getDefaultMessage(),
                errorCode.getStatus().value(),
                path,
                traceId,
                OffsetDateTime.now(),
                fieldErrors
        );
    }

    public record FieldErrorDetail(String field, String reason) {}
}
