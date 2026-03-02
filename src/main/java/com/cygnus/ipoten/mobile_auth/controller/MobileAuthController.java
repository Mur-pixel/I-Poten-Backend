package com.cygnus.ipoten.mobile_auth.controller;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.mobile_auth.controller.dto.MobileRefreshRequest;
import com.cygnus.ipoten.mobile_auth.controller.dto.MobileRefreshResponse;
import com.cygnus.ipoten.mobile_auth.entity.AccountRefreshToken;
import com.cygnus.ipoten.mobile_auth.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {

    private final RefreshTokenService refreshTokenService;
    private final AuthenticationService authenticationService;
    private final AccountProfileService accountProfileService;

    /**
     * refreshToken으로 새 accessToken + 새 refreshToken 발급 (token rotation)
     */
    @PostMapping("/refresh")
    public ResponseEntity<MobileRefreshResponse> refresh(
            @RequestBody MobileRefreshRequest request,
            HttpServletResponse response
    ) {
        Optional<AccountRefreshToken> tokenOpt = refreshTokenService.validate(request.getRefreshToken());
        if (tokenOpt.isEmpty()) {
            log.info("[MobileAuth] 유효하지 않은 리프레시 토큰");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AccountRefreshToken tokenEntity = tokenOpt.get();
        Long accountId = tokenEntity.getAccount().getId();

        String newAccessToken = authenticationService.createUserTokenWithAccessToken(accountId, "mobile");
        String newRefreshToken = refreshTokenService.rotate(tokenEntity);

        response.addHeader("Set-Cookie", String.format(
                "userToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict",
                newAccessToken, 6 * 60 * 60
        ));

        AccountProfile profile = accountProfileService.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("프로필 없음"));

        log.info("[MobileAuth] 토큰 갱신 완료 - accountId: {}", accountId);
        return ResponseEntity.ok(new MobileRefreshResponse(newAccessToken, newRefreshToken, profile.getNickname()));
    }

    /**
     * 회원가입 완료 후 refreshToken 발급
     * 기존 signup 코드 수정 없이, 회원가입 직후 Flutter에서 이 엔드포인트를 호출
     */
    @PostMapping("/register")
    public ResponseEntity<MobileRefreshResponse> register(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        if (userToken == null || userToken.isBlank() || userToken.startsWith("Temporary_")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean valid = authenticationService.verification(userToken);
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long accountId = authenticationService.getAccountIdByUserToken(userToken);
        AccountProfile profile = accountProfileService.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("프로필 없음"));

        Account account = profile.getAccount();
        String refreshToken = refreshTokenService.createOrReplace(account);

        log.info("[MobileAuth] 회원가입 후 refreshToken 발급 - accountId: {}", accountId);
        return ResponseEntity.ok(new MobileRefreshResponse(userToken, refreshToken, profile.getNickname()));
    }

    /**
     * 로그아웃: accessToken 세션 삭제 + refreshToken 폐기
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody(required = false) MobileRefreshRequest request,
            HttpServletResponse response
    ) {
        Cookie cookie = new Cookie("userToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        if (userToken != null && !userToken.isBlank()) {
            authenticationService.logout(userToken);
        }
        if (request != null && request.getRefreshToken() != null) {
            refreshTokenService.revoke(request.getRefreshToken());
        }

        log.info("[MobileAuth] 로그아웃 완료");
        return ResponseEntity.ok("success");
    }
}
