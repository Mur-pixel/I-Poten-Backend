package com.cygnus.iptn.authentication.controller;

import com.cygnus.iptn.accountProfile.entity.AccountProfile;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.authentication.controller.response_form.TokenAuthenticationExpiredResponseForm;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import com.cygnus.iptn.common.annotation.PublicEndpoint;
import com.cygnus.iptn.common.util.CookieUtil;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/authentication")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AccountProfileService accountProfileService;
    private final RedisCacheService redisCacheService;

    // Temporary_ 토큰 분기 등 자체 검증 로직이 있어 @PublicEndpoint 처리
    @PublicEndpoint
    @GetMapping("/token/verification")
    public ResponseEntity<TokenAuthenticationExpiredResponseForm> verifyToken(
            @CookieValue(name = "userToken", required = false) String userToken) {

        log.info("토큰 검증 시작");

        if (userToken == null || userToken.isBlank() || userToken.startsWith(CookieUtil.TEMPORARY_TOKEN_PREFIX)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenAuthenticationExpiredResponseForm(false));
        }

        boolean verification = authenticationService.verification(userToken);

        if (verification) {
            Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
            AccountProfile accountProfile = accountProfileService.findByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("회원 검증 중 회원을 찾을 수 없음 "));
            return ResponseEntity.ok(new TokenAuthenticationExpiredResponseForm(true, accountProfile.getNickname()));
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new TokenAuthenticationExpiredResponseForm(false));
    }

    // Temporary_ 토큰 분기 및 쿠키 직접 삭제 로직이 있어 @PublicEndpoint 처리
    @PublicEndpoint
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(name = "userToken", required = false) String userToken,
            HttpServletResponse response) {

        log.info("로그아웃 호출");

        try {
            if (userToken == null || userToken.isEmpty() || userToken.startsWith(CookieUtil.TEMPORARY_TOKEN_PREFIX)) {
                log.info("토큰 없음");
                return ResponseEntity.badRequest().body("fail: no token");
            }

            CookieUtil.clearUserToken(response);

            boolean logoutResult = authenticationService.logout(userToken);
            if (logoutResult) {
                return ResponseEntity.ok("success");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("fail: invalid token");
            }
        } catch (Exception e) {
            log.error("로그아웃 처리 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail: server error");
        }
    }
}
