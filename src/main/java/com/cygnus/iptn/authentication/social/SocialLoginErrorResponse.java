package com.cygnus.iptn.authentication.social;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Getter
public class SocialLoginErrorResponse {

    // 에러 코드 (Enum 기반 → 프론트에서 분기 처리 용도)
    // ex) ACCOUNT_WITHDRAWN
    private final String code;

    // 사용자에게 보여줄 에러 메시지
    private final String message;

    // 어떤 소셜 로그인에서 발생했는지 (GOOGLE, KAKAO 등)
    // 프론트에서 UI/문구 분기 처리에 활용 가능
    private final String provider;

    // 재가입 가능 시점 (탈퇴 계정인 경우에만 사용)
    // 프론트에서 "OO일까지 재가입 불가" 안내에 활용
    private final Instant rejoinAvailableAt;

    // HTTP 상태 코드 (400, 401, 409 등)
    // 클라이언트에서 상태 기반 처리 가능
    private final int status;

    public SocialLoginErrorResponse(
            SocialLoginErrorCode code,
            String message,
            String provider,
            Instant rejoinAvailableAt,
            HttpStatus status
    ) {
        // Enum을 문자열로 변환하여 응답
        // → 프론트와 API 계약을 단순화
        this.code = code.name();

        this.message = message;
        this.provider = provider;

        // 특정 에러(탈퇴 계정 등)에만 값이 존재할 수 있음 (nullable)
        this.rejoinAvailableAt = rejoinAvailableAt;

        // HttpStatus → int 변환하여 응답
        // → 프론트에서 바로 사용 가능하도록 단순화
        this.status = status.value();
    }
}