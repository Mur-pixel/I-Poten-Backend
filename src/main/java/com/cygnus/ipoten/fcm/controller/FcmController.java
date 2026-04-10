package com.cygnus.ipoten.fcm.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.fcm.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/token")
    public ResponseEntity<Void> registerToken(
            @LoginUser Long accountId,
            @RequestBody FcmTokenRequest request) {

        fcmTokenService.registerToken(accountId, request.getToken());
        return ResponseEntity.ok().build();
    }
}
