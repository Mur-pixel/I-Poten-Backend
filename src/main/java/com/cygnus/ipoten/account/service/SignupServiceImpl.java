package com.cygnus.ipoten.account.service;

import com.cygnus.ipoten.account.controller.request_form.RegisterRequestForm;
import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.service.register_response.RegisterResponse;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.credit.event.AccountSignedUpEvent;
import com.cygnus.ipoten.infrastructure.external.email.EmailService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Account account = accountService.createAccount(registerRequestForm.toRegisterAccountRequest())
                .orElseThrow(() ->
                        new IllegalArgumentException("Account 생성 실패")
                );

        AccountProfile accountProfile = accountProfileService.createAccountProfile(account, registerRequestForm.toRegisterAccountProfileRequestForm())
                .orElseThrow(() ->
                        new IllegalArgumentException("AccountProfile 생성 실패")
                );


        String userToken = UUID.randomUUID().toString();
        redisCacheService.setKeyAndValue(account.getId(), accessToken);
        redisCacheService.setKeyAndValue(userToken, account.getId());
        authenticationService.deleteToken(tempToken);

        emailService.sendSignupWelcomeEmail(accountProfile.getEmail(), accountProfile.getNickname());

        log.info("회원가입 중 ....");
        applicationEventPublisher.publishEvent(
                new AccountSignedUpEvent(account.getId())
        );


        return new RegisterResponse(accountProfile.getNickname(), accountProfile.getEmail(), userToken);
    }
}
