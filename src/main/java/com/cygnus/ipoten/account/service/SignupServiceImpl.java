package com.cygnus.ipoten.account.service;

import com.cygnus.ipoten.account.controller.request_form.RegisterRequestForm;
import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.service.register_response.RegisterResponse;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.infrastructure.external.email.EmailService;
import com.cygnus.ipoten.profileAppearance.Service.ProfileAppearanceService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private final AccountService accountService;
    private final AccountProfileService accountProfileService;
    private final RedisCacheService redisCacheService;
    private final ProfileAppearanceService profileAppearanceService;
    private final AuthenticationService authenticationService;
    private final EmailService emailService;


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


        return new RegisterResponse(accountProfile.getNickname(), accountProfile.getEmail(), userToken);
    }
}
