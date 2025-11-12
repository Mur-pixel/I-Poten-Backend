package com.cygnus.ipoten.naver_authentication.service;


import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.config.FrontendConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NaverAuthenticationServiceImpl implements NaverAuthenticationService {

    private final String loginUrl;
    private final String clientId;
    private final String redirectUri;
    private final FrontendConfig frontendConfig;
    private final AuthenticationService authenticationService;
    private final AccountProfileService accountProfileService;


    public NaverAuthenticationServiceImpl(
            @Value("${naver.login-url}") String loginUrl,
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.redirect-uri}") String redirectUri,
            FrontendConfig frontendConfig,
            AuthenticationService authenticationService,
            AccountProfileService accountProfileService) {

        this.loginUrl = loginUrl;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.frontendConfig = frontendConfig;
        this.authenticationService = authenticationService;
        this.accountProfileService = accountProfileService;
    }

    @Override
    public String link() {
        return String.format(
                "%s?client_id=%s&response_type=code&redirect_uri=%s&state=RANDOM_STRING", loginUrl, clientId,redirectUri);
    }
}
