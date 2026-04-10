package com.cygnus.ipoten.meta_authentication.service;

import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.authentication.social.SocialLoginPolicyService;
import com.cygnus.ipoten.config.FrontendConfig;
import com.cygnus.ipoten.meta_authentication.service.response.MetaLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class MetaAuthenticationServiceImpl implements MetaAuthenticationService {

    private final String loginUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenRequestUri;
    private final String userInfoRequestUri;
    private final RestTemplate restTemplate;
    private final FrontendConfig frontendConfig;
    private final AuthenticationService authenticationService;
    private final AccountProfileService accountProfileService;
    private final SocialLoginPolicyService socialLoginPolicyService;

    public MetaAuthenticationServiceImpl(
            @Value("${meta.login-url}") String loginUrl,
            @Value("${meta.client-id}") String clientId,
            @Value("${META_CLIENT_SECRET}") String clientSecret,
            @Value("${meta.redirect-uri}") String redirectUri,
            @Value("${meta.token-request-uri}") String tokenRequestUri,
            @Value("${meta.user-info-request-uri}") String userInfoRequestUri,
            RestTemplate restTemplate,
            FrontendConfig frontendConfig,
            AuthenticationService authenticationService,
            AccountProfileService accountProfileService,
            SocialLoginPolicyService socialLoginPolicyService) {

        this.loginUrl = loginUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.tokenRequestUri = tokenRequestUri;
        this.userInfoRequestUri = userInfoRequestUri;
        this.restTemplate = restTemplate;
        this.frontendConfig = frontendConfig;
        this.authenticationService = authenticationService;
        this.accountProfileService = accountProfileService;
        this.socialLoginPolicyService = socialLoginPolicyService;
    }

    @Override
    public String requestKakaoOauthLink() {
        return String.format(
                "%s?client_id=%s&redirect_uri=%s&scope=email,public_profile,&response_type=code&scope=email,",
                loginUrl, clientId, redirectUri
        );
    }

    @Override
    public MetaLoginResponse handleLogin(String code) {
        String origin = frontendConfig.getOrigins().get(0);
        String accessToken = getAccessToken(code);
        Map<String, Object> userInfo = getUserInfo(accessToken);
        String email = (String) userInfo.get("email");
        String nickName = (String) userInfo.get("name");

        var loginResult = socialLoginPolicyService.login(email, LoginType.META, accessToken);
        return MetaLoginResponse.of(
                loginResult.isNewUser(),
                loginResult.isRejoinUser(),
                loginResult.token(),
                nickName,
                email,
                origin
        );
    }

    @Override
    public String getAccessToken(String code) {
        String tokenUrl = "https://graph.facebook.com/v19.0/oauth/access_token" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&client_secret=" + clientSecret +
                "&code=" + code;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.getForEntity(tokenUrl, Map.class);

        Map<String, Object> body = response.getBody();
        return (String) body.get("access_token");
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        String url = "https://graph.facebook.com/v19.0/me?fields=name,email&access_token=" + accessToken;
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, Map.class);
    }
}
