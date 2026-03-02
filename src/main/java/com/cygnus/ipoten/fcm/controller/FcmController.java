package com.cygnus.ipoten.fcm.controller;

import com.cygnus.ipoten.fcm.service.FcmTokenService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmTokenService fcmTokenService;
    private final RedisCacheService redisCacheService;

    @PostMapping("/token")
    public ResponseEntity<Void> registerToken(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody FcmTokenRequest request) {
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        if (accountId == null) {
            return ResponseEntity.status(401).build();
        }
        fcmTokenService.registerToken(accountId, request.getToken());
        return ResponseEntity.ok().build();
    }
}
