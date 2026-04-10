package com.cygnus.iptn.account.service;

import com.cygnus.iptn.account.controller.request_form.RegisterRequestForm;
import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.account.service.register_response.RegisterResponse;
import com.cygnus.iptn.accountProfile.entity.AccountProfile;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import com.cygnus.iptn.authentication.social.SocialLoginException;
import com.cygnus.iptn.credit.event.AccountSignedUpEvent;
import com.cygnus.iptn.infrastructure.external.email.EmailService;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SignupServiceImpl implements SignupService {

    private final AccountService accountService;
    private final AccountProfileService accountProfileService;
    private final RedisCacheService redisCacheService;
    private final AuthenticationService authenticationService;
    private final EmailService emailService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public RegisterResponse signup(String tempToken, RegisterRequestForm registerRequestForm) {
        String accessToken = redisCacheService.getValueByKey(tempToken, String.class);

        // 동일 이메일 + 동일 로그인 타입 기준 기존 프로필 존재 여부 확인
        // 이미 가입한 회원인지, 탈퇴 후 재가입 대상인지 판별하기 위한 조회
        Optional<AccountProfile> existingProfile = accountProfileService.loadProfileByEmailAndLoginType(
                registerRequestForm.getEmail(),
                registerRequestForm.getLoginType()
        );

        // 기존 계정이 있으면 일반 신규가입이 아니라 재가입 가능 여부를 확인한 뒤 재활성화 또는 가입 거절 처리
        if (existingProfile.isPresent()) {
            return rejoinOrReject(tempToken, accessToken, registerRequestForm, existingProfile.get());
        }

        // 신규 계정 생성
        Account account = accountService.createAccount(registerRequestForm.toRegisterAccountRequest())
                .orElseThrow(() -> new IllegalArgumentException("Account 생성 실패"));

        // 신규 프로필 생성
        AccountProfile accountProfile = accountProfileService.createAccountProfile(
                account,
                registerRequestForm.toRegisterAccountProfileRequestForm()
        ).orElseThrow(() -> new IllegalArgumentException("AccountProfile 생성 실패"));

        // 실제 로그인 상태에서 사용할 userToken 발급
        String userToken = UUID.randomUUID().toString();

        // Redis 에 계정과 accessToken 매핑 저장
        // accountId -> accessToken
        redisCacheService.setKeyAndValue(account.getId(), accessToken);

        // Redis 에 userToken 과 accountId 매핑 저장
        // userToken -> accountId
        redisCacheService.setKeyAndValue(userToken, account.getId());

        // 회원가입 전용 임시 토큰 삭제
        authenticationService.deleteToken(tempToken);

        // 회원가입 환영 메일 발송
        emailService.sendSignupWelcomeEmail(accountProfile.getEmail(), accountProfile.getNickname());

        // 회원가입 완료 이벤트 발행
        // 크레딧 지급, 통계 적재, 로그 기록 등
        applicationEventPublisher.publishEvent(new AccountSignedUpEvent(account.getId()));

        return new RegisterResponse(accountProfile.getNickname(), accountProfile.getEmail(), userToken);
    }

    private RegisterResponse rejoinOrReject(
            String tempToken,
            String accessToken,
            RegisterRequestForm registerRequestForm,
            AccountProfile existingProfile
    ) {
        // 기존 프로필에 연결된 계정 조회
        Account existingAccount = existingProfile.getAccount();

        // 예외 메시지/정책 처리에 사용할 로그인 타입
        LoginType loginType = registerRequestForm.getLoginType();

        // 이미 존재하는 계정이 탈퇴 상태가 아니라면 동일 이메일/로그인타입으로 중복 가입 시도이므로 차단
        if (!existingAccount.isWithdrawn()) {
            throw new IllegalArgumentException("이미 가입된 회원입니다.");
        }

        // 탈퇴한 계정이어도 재가입 대기 기간이 지나지 않았다면 가입 불가
        if (!existingAccount.canRejoinAt(Instant.now())) {
            throw SocialLoginException.accountWithdrawn(loginType, existingAccount.getRejoinAvailableAt());
        }

        // 재가입 가능하면 계정을 다시 활성화
        existingAccount.reactivate();

        // 재가입 시 최신 입력값으로 프로필 갱신
        existingProfile.updateProfile(registerRequestForm.getNickname(), registerRequestForm.getEmail());

        // 실제 로그인에 사용할 userToken 재발급
        String userToken = UUID.randomUUID().toString();

        // 계정과 accessToken 연결 저장
        redisCacheService.setKeyAndValue(existingAccount.getId(), accessToken);

        // userToken 과 accountId 연결 저장
        redisCacheService.setKeyAndValue(userToken, existingAccount.getId());

        // 더 이상 필요 없는 임시 토큰 삭제
        authenticationService.deleteToken(tempToken);

        // 재가입 완료 응답 반환
        return new RegisterResponse(existingProfile.getNickname(), existingProfile.getEmail(), userToken);
    }
}
