package com.cygnus.iptn.authentication.social;

import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.accountProfile.entity.AccountProfile;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocialLoginPolicyService {

    // 이메일 + 로그인 타입 기준으로 기존 회원 프로필을 조회하는 서비스
    private final AccountProfileService accountProfileService;

    // 임시 토큰 / 사용자 토큰 발급을 담당하는 인증 서비스
    private final AuthenticationService authenticationService;

    public SocialLoginResult login(String email, LoginType loginType, String accessToken) {

        // 1. 동일 이메일 + 동일 로그인 타입으로 가입된 계정이 있는지 조회
        Optional<AccountProfile> accountProfile =
                accountProfileService.loadProfileByEmailAndLoginType(email, loginType);

        log.info(
                "Social login lookup - provider: {}, email: {}, matchedByEmailAndLoginType: {}",
                loginType,
                maskEmail(email),
                accountProfile.isPresent()
        );

        // 2. 가입 이력이 없으면 신규 회원 후보로 판단
        // 아직 정식 가입 전이므로 임시 사용자 토큰(temp token) 발급
        if (accountProfile.isEmpty()) {
            Optional<AccountProfile> emailOnlyProfile = accountProfileService.loadProfileByEmail(email);
            if (emailOnlyProfile.isPresent()) {
                var existingAccount = emailOnlyProfile.get().getAccount();
                log.warn(
                        "Social login fallback match by email only - provider: {}, email: {}, accountId: {}, accountStatus: {}, withdrawnAt: {}, isWithdrawn: {}",
                        loginType,
                        maskEmail(email),
                        existingAccount.getId(),
                        existingAccount.getStatus(),
                        existingAccount.getWithdrawnAt(),
                        existingAccount.isWithdrawn()
                );
            } else {
                log.info("Social login treated as new user - provider: {}, email: {}", loginType, maskEmail(email));
            }

            String tempToken = authenticationService.createTemporaryUserTokenWithAccessToken(accessToken);
            log.info(
                    "Social login issued temporary token - provider: {}, email: {}, tokenPrefix: {}",
                    loginType,
                    maskEmail(email),
                    tokenPrefix(tempToken)
            );

            return new SocialLoginResult(
                    true,
                    false,
                    tempToken,
                    null
            );
        }

        // 3. 기존 계정이 존재하면 연결된 account 조회
        var account = accountProfile.get().getAccount();
        log.info(
                "Social login matched account - provider: {}, email: {}, accountId: {}, accountStatus: {}, isWithdrawn: {}, withdrawnAt: {}",
                loginType,
                maskEmail(email),
                account.getId(),
                account.getStatus(),
                account.isWithdrawn(),
                account.getWithdrawnAt()
        );

        // 4. 탈퇴한 계정인 경우 재가입 가능 여부 확인
        if (account.isWithdrawn()) {
            Instant now = Instant.now();
            Instant rejoinAvailableAt = account.getRejoinAvailableAt();
            log.info(
                    "Social login withdrawn account check - provider: {}, email: {}, accountId: {}, withdrawnAt: {}, rejoinAvailableAt: {}, now: {}",
                    loginType,
                    maskEmail(email),
                    account.getId(),
                    account.getWithdrawnAt(),
                    rejoinAvailableAt,
                    now
            );

            if (!account.canRejoinAt(now)) {
                log.warn(
                        "Social login blocked for withdrawn account - provider: {}, email: {}, accountId: {}, rejoinAvailableAt: {}",
                        loginType,
                        maskEmail(email),
                        account.getId(),
                        rejoinAvailableAt
                );
                throw SocialLoginException.accountWithdrawn(loginType, rejoinAvailableAt);
            }

            String tempToken = authenticationService.createTemporaryUserTokenWithAccessToken(accessToken);
            log.info(
                    "Social login rejoin eligible - provider: {}, email: {}, accountId: {}, tokenPrefix: {}",
                    loginType,
                    maskEmail(email),
                    account.getId(),
                    tokenPrefix(tempToken)
            );

            return new SocialLoginResult(
                    true,
                    true,
                    tempToken,
                    account
            );
        }

        // 5. 활성 계정이면 바로 로그인 처리
        // 정식 사용자 토큰(user token) 발급
        String userToken = authenticationService.createUserTokenWithAccessToken(account.getId(), accessToken);
        log.info(
                "Social login issued user token - provider: {}, email: {}, accountId: {}, tokenPrefix: {}",
                loginType,
                maskEmail(email),
                account.getId(),
                tokenPrefix(userToken)
        );

        return new SocialLoginResult(
                false,
                false,
                userToken,
                account
        );
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String tokenPrefix(String token) {
        if (token == null || token.isBlank()) {
            return token;
        }
        return token.substring(0, Math.min(token.length(), 12));
    }
}
