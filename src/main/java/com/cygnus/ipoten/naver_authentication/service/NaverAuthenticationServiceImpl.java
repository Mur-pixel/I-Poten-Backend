package com.cygnus.ipoten.naver_authentication.service;

import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.authentication.social.SocialLoginPolicyService;
import com.cygnus.ipoten.config.FrontendConfig;
import com.cygnus.ipoten.mobile_auth.service.RefreshTokenService;
import com.cygnus.ipoten.naver_authentication.service.mobile_response.NaverLoginMobileResponse;
import com.cygnus.ipoten.naver_authentication.service.response.NaverLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
public class NaverAuthenticationServiceImpl implements NaverAuthenticationService {

    private final String loginUrl;
    private final String clientId;
    private final String redirectUri;
    private final String clientSecret;
    private final RestTemplate restTemplate;
    private final FrontendConfig frontendConfig;
    private final AuthenticationService authenticationService;
    private final AccountProfileService accountProfileService;
    private final RefreshTokenService refreshTokenService;
    private final SocialLoginPolicyService socialLoginPolicyService;

    public NaverAuthenticationServiceImpl(
            @Value("${naver.login-url}") String loginUrl,
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.client-secret}") String clientSecret,
            @Value("${naver.redirect-uri}") String redirectUri,
            RestTemplate restTemplate,
            FrontendConfig frontendConfig,
            AuthenticationService authenticationService,
            AccountProfileService accountProfileService,
            RefreshTokenService refreshTokenService,
            SocialLoginPolicyService socialLoginPolicyService) {

        this.loginUrl = loginUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.restTemplate = restTemplate;
        this.frontendConfig = frontendConfig;
        this.authenticationService = authenticationService;
        this.accountProfileService = accountProfileService;
        this.refreshTokenService = refreshTokenService;
        this.socialLoginPolicyService = socialLoginPolicyService;
    }

    @Override
    public String link() {
        return String.format(
                "%s?client_id=%s&response_type=code&redirect_uri=%s&state=RANDOM_STRING",
                loginUrl, clientId, redirectUri);
    }

    @Override
    public NaverLoginResponse handleLogin(String code) {
        String origin = frontendConfig.getOrigins().get(0);
        String accessToken = getAccessToken(code);
        Map<String, Object> userInfo = getUserInfo(accessToken);
        String email = (String) userInfo.get("email");
        String nickname = (String) userInfo.get("nickname");

        var loginResult = socialLoginPolicyService.login(email, LoginType.NAVER, accessToken);
        return NaverLoginResponse.of(
                loginResult.isNewUser(),
                loginResult.isRejoinUser(),
                loginResult.token(),
                nickname,
                email,
                origin
        );
    }

    @Override
    public NaverLoginMobileResponse handleLoginMobile(String accessToken) {
        Map<String, Object> userInfo = getUserInfo(accessToken);
        String email = (String) userInfo.get("email");
        String nickname = (String) userInfo.get("nickname");

        var loginResult = socialLoginPolicyService.login(email, LoginType.NAVER, accessToken);
        if (loginResult.isNewUser()) {
            return new NaverLoginMobileResponse(true, loginResult.token(), nickname, email, loginResult.isRejoinUser());
        }

        var account = loginResult.account();
        String refreshToken = refreshTokenService.createOrReplace(account);
        return new NaverLoginMobileResponse(false, loginResult.token(), nickname, email, refreshToken);
    }

    @Override
    public String getAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        String url = UriComponentsBuilder.fromHttpUrl("https://nid.naver.com/oauth2.0/token")
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code", code)
                .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> body = response.getBody();
        return (String) body.get("access_token");
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + accessToken);

        HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                httpEntity,
                Map.class
        );
        Map<String, Object> body = response.getBody();
        return (Map<String, Object>) body.get("response");
    }
}
