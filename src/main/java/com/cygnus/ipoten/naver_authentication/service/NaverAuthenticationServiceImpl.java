package com.cygnus.ipoten.naver_authentication.service;


import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.config.FrontendConfig;
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
import java.util.Optional;

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


    public NaverAuthenticationServiceImpl(
            @Value("${naver.login-url}") String loginUrl,
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.client-secret}") String clientSecret,
            @Value("${naver.redirect-uri}") String redirectUri,
            RestTemplate restTemplate,
            FrontendConfig frontendConfig,
            AuthenticationService authenticationService,
            AccountProfileService accountProfileService) {

        this.loginUrl = loginUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.restTemplate = restTemplate;
        this.frontendConfig = frontendConfig;
        this.authenticationService = authenticationService;
        this.accountProfileService = accountProfileService;
    }

    @Override
    public String link() {
        log.info("redirect_uri는? : {}", redirectUri);
        return String.format(
                "%s?client_id=%s&response_type=code&redirect_uri=%s&state=RANDOM_STRING", loginUrl, clientId,redirectUri);
    }

    @Override
    public NaverLoginResponse handleLogin(String code) {

        String origin = frontendConfig.getOrigins().get(0);
        String accessToken = getAccessToken(code);
        Map<String, Object> userInfo = getUserInfo(accessToken);
        String email = (String) userInfo.get("email");
        String nickname = (String) userInfo.get("nickname");

        Optional<AccountProfile> accountProfile = accountProfileService.loadProfileByEmailAndLoginType(email, LoginType.NAVER);

        boolean isNewUser = accountProfile.isEmpty();

        String token = isNewUser
                ? authenticationService.createTemporaryUserTokenWithAccessToken(accessToken)
                : authenticationService.createUserTokenWithAccessToken(accountProfile.get().getAccount().getId(), accessToken);


        return NaverLoginResponse.of(isNewUser, token, nickname, email, origin);
    }

    @Override
    public String getAccessToken(String code) {

        RestTemplate restTemplate = new RestTemplate();

        // 네이버 토큰 요청 URL 구성
        String url = UriComponentsBuilder.fromHttpUrl("https://nid.naver.com/oauth2.0/token")
                .queryParam("grant_type", "authorization_code")
                .queryParam("client_id", clientId)
                .queryParam("client_secret", clientSecret)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code", code)
                .toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        Map<String, Object> body = response.getBody();
        String accessToken = (String) body.get("access_token");


        return accessToken;
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {

        String getUserInfoRequestUrl = String.format(
                "https://openapi.naver.com/v1/nid/me"
        );

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + accessToken);

        HttpEntity<Object> httpEntity = new HttpEntity<>(httpHeaders);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response = restTemplate.exchange(getUserInfoRequestUrl, HttpMethod.GET, httpEntity, Map.class);
        Map<String, Object> body = response.getBody();
        return (Map<String, Object>) body.get("response");
    }


}
