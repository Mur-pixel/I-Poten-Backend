package com.cygnus.ipoten.authentication.social;

import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
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

        // 2. 가입 이력이 없으면 신규 회원 후보로 판단
        // 아직 정식 가입 전이므로 임시 사용자 토큰(temp token) 발급
        if (accountProfile.isEmpty()) {
            return new SocialLoginResult(
                    true,   // 회원가입 필요
                    false,  // 재가입 아님
                    authenticationService.createTemporaryUserTokenWithAccessToken(accessToken),
                    null    // 연결된 기존 계정 없음
            );
        }

        // 3. 기존 계정이 존재하면 연결된 account 조회
        var account = accountProfile.get().getAccount();

        // 4. 탈퇴한 계정인 경우 재가입 가능 여부 확인
        if (account.isWithdrawn()) {
            Instant now = Instant.now();

            // 재가입 대기 기간이 지나지 않았다면 예외 발생
            if (!account.canRejoinAt(now)) {
                throw SocialLoginException.accountWithdrawn(loginType, account.getRejoinAvailableAt());
            }

            // 재가입 가능한 상태라면
            // 회원가입 화면으로 다시 보내기 위해 임시 토큰 발급
            return new SocialLoginResult(
                    true,   // 회원가입 절차 필요
                    true,   // 재가입 대상
                    authenticationService.createTemporaryUserTokenWithAccessToken(accessToken),
                    account // 기존 탈퇴 계정 정보 전달
            );
        }

        // 5. 활성 계정이면 바로 로그인 처리
        // 정식 사용자 토큰(user token) 발급
        return new SocialLoginResult(
                false,  // 회원가입 필요 없음
                false,  // 재가입 아님
                authenticationService.createUserTokenWithAccessToken(account.getId(), accessToken),
                account // 로그인된 기존 계정
        );
    }
}