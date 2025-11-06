package com.cygnus.ipoten.authentication.controller;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.controller.response_form.TokenAuthenticationExpiredResponseForm;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import jakarta.servlet.http.Cookie;
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


    @GetMapping("/token/verification")
    public ResponseEntity<TokenAuthenticationExpiredResponseForm> verifyToken(
            @CookieValue(name = "userToken", required = false) String userToken) {

        log.info("");
        log.info("토큰 검증 시작");
        log.info("");


        if (userToken == null || userToken.isBlank() || userToken.startsWith("Temporary_")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenAuthenticationExpiredResponseForm(false));
        }

        boolean verification = authenticationService.verification(userToken);

        if (verification) {

            Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
            AccountProfile accountProfile = accountProfileService.findByAccountId(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("회원 검증 중 회원을 찾을 수 없음 "));
            String nickname = accountProfile.getNickname();
            return ResponseEntity.ok(new TokenAuthenticationExpiredResponseForm(true, nickname));
        } else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenAuthenticationExpiredResponseForm(false));
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(name = "userToken", required = false) String userToken,
            HttpServletResponse response) {
        log.info("로그아웃 호출");

        try {
            if (userToken == null || userToken.isEmpty() || userToken.startsWith("Temporary_")) {
                log.info("토큰 없음");
                return ResponseEntity.badRequest().body("fail: no token");
            }

            // 쿠키 삭제
            Cookie cookie = new Cookie("userToken", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);

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
