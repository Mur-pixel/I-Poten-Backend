package com.cygnus.iptn.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 내부 서비스 간 통신에서 사용하는 API 키 검증 유틸리티.
 * 회원탈퇴 오케스트레이터 등 내부 호출 전용 엔드포인트에서 사용합니다.
 */
@Component
public class InternalApiKeyValidator {

    @Value("${internal.api.key:}")
    private String internalApiKey;

    /**
     * X-Internal-Key 헤더 값이 설정된 키와 일치하는지 검증합니다.
     *
     * @param headerValue 요청 헤더에서 추출한 X-Internal-Key 값
     * @return 일치 여부
     */
    public boolean isValid(String headerValue) {
        return !internalApiKey.isBlank() && internalApiKey.equals(headerValue);
    }
}
