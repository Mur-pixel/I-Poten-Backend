package com.cygnus.ipoten.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 표현하는 베이스 예외.
 *
 * <p>도메인 코드에서는 {@link RuntimeException} 을 직접 던지는 대신
 * 의미를 가진 {@link ErrorCode} 와 함께 본 예외(또는 그 서브클래스)를 던진다.
 * {@code GlobalExceptionHandler} 가 이를 표준 {@link ErrorResponse} 로 변환한다.
 *
 * <pre>
 *  throw new BusinessException(ErrorCode.CREDIT_INSUFFICIENT);
 *  throw new BusinessException(ErrorCode.WORDBOOK_NOT_FOUND, "id=" + id);
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String overrideMessage) {
        super(overrideMessage != null ? overrideMessage : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String overrideMessage, Throwable cause) {
        super(overrideMessage != null ? overrideMessage : errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
    }
}
