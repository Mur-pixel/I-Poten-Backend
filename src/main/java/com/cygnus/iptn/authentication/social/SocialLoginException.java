package com.cygnus.iptn.authentication.social;

import com.cygnus.iptn.account.entity.LoginType;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Getter
public class SocialLoginException extends RuntimeException {

    // 소셜 로그인 실패 원인 코드
    // ex) ACCOUNT_WITHDRAWN
    private final SocialLoginErrorCode code;

    // 어떤 소셜 로그인 제공자인지 나타냄
    // ex) GOOGLE, KAKAO, NAVER
    private final LoginType provider;

    // HTTP 응답 상태 코드
    private final HttpStatus status;

    // 재가입 가능 시점
    // 탈퇴 계정 관련 예외에서만 사용될 수 있음
    private final Instant rejoinAvailableAt;

    public SocialLoginException(
            SocialLoginErrorCode code,
            LoginType provider,
            String message,
            HttpStatus status,
            Instant rejoinAvailableAt
    ) {
        // RuntimeException 메시지로 부모 클래스에 전달
        super(message);
        this.code = code;
        this.provider = provider;
        this.status = status;
        this.rejoinAvailableAt = rejoinAvailableAt;
    }

    // 예외 객체를 클라이언트 응답 DTO 로 변환
    // ControllerAdvice 등에서 바로 사용할 수 있도록 변환 책임을 내부에 둠
    public SocialLoginErrorResponse toResponse() {
        return new SocialLoginErrorResponse(
                code,
                getMessage(),
                provider.name(),
                rejoinAvailableAt,
                status
        );
    }

    // 탈퇴 계정 로그인/재가입 제한 상황용 정적 팩토리 메서드
    public static SocialLoginException accountWithdrawn(LoginType provider, Instant rejoinAvailableAt) {
        return new SocialLoginException(
                SocialLoginErrorCode.ACCOUNT_WITHDRAWN,
                provider,
                "탈퇴한 계정입니다.",
                HttpStatus.CONFLICT,
                rejoinAvailableAt
        );
    }
}