package com.cygnus.ipoten.mobile_auth.controller;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.common.annotation.LoginToken;
import com.cygnus.ipoten.common.annotation.PublicEndpoint;
import com.cygnus.ipoten.common.util.CookieUtil;
import com.cygnus.ipoten.mobile_auth.controller.dto.MobileRefreshRequest;
import com.cygnus.ipoten.mobile_auth.controller.dto.MobileRefreshResponse;
import com.cygnus.ipoten.mobile_auth.entity.AccountRefreshToken;
import com.cygnus.ipoten.mobile_auth.service.RefreshTokenService;
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

    // refreshToken 기반 갱신 — userToken 쿠키 불필요
    @PublicEndpoint
    @PostMapping("/refresh")
    public ResponseEntity<MobileRefreshResponse> refresh(
            @RequestBody MobileRefreshRequest request,
            HttpServletResponse response) {

        Optional<AccountRefreshToken> tokenOpt = refreshTokenService.validate(request.getRefreshToken());
        if (tokenOpt.isEmpty()) {
            log.info("[MobileAuth] 유효하지 않은 리프레시 토큰");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AccountRefreshToken tokenEntity = tokenOpt.get();
        Account account = tokenEntity.getAccount();
        account.ensureActive();
        Long accountId = account.getId();

        String newAccessToken = authenticationService.createUserTokenWithAccessToken(accountId, "mobile");
        String newRefreshToken = refreshTokenService.rotate(tokenEntity);

        CookieUtil.addUserToken(response, newAccessToken);

        AccountProfile profile = accountProfileService.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("프로필 없음"));

        log.info("[MobileAuth] 토큰 갱신 완료 - accountId: {}", accountId);
        return ResponseEntity.ok(new MobileRefreshResponse(newAccessToken, newRefreshToken, profile.getNickname()));
    }

    // 회원가입 완료 후 refreshToken 발급 — 인터셉터가 userToken 검증
    @PostMapping("/register")
    public ResponseEntity<MobileRefreshResponse> register(
            @LoginUser Long accountId,
            @LoginToken String userToken) {

        AccountProfile profile = accountProfileService.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalStateException("프로필 없음"));

        Account account = profile.getAccount();
        account.ensureActive();
        String refreshToken = refreshTokenService.createOrReplace(account);

        log.info("[MobileAuth] 회원가입 후 refreshToken 발급 - accountId: {}", accountId);
        return ResponseEntity.ok(new MobileRefreshResponse(userToken, refreshToken, profile.getNickname()));
    }

    // 로그아웃 — userToken 선택적 (쿠키 없어도 refreshToken만으로 로그아웃 가능)
    @PublicEndpoint
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody(required = false) MobileRefreshRequest request,
            HttpServletResponse response) {

        CookieUtil.clearUserToken(response);

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
