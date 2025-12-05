package com.cygnus.ipoten.userDashboard.service;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenAccountService {

    private final RedisCacheService redisCacheService;

    public Long resolveAccountId(String token){
        log.info("🔑 Cookie 토큰 수신: {}", token);

        if (token == null || token.isEmpty()) {
            log.warn("⚠️ 쿠키에 userToken이 없습니다.");
            return null; // 예외 던지지 말고 null 반환
        }

        Long accountId = redisCacheService.getValueByKey(token, Long.class);
        log.info("✅ Redis 조회 결과: accountId = {}", accountId);

        if (accountId == null) {
            log.warn("❌ 유효하지 않은 토큰이거나 만료됨: {}", token);
            return null; // 예외 대신 null
        }

        return accountId;
    }
}
