package com.cygnus.ipoten.google_authentication.service;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.config.FrontendConfig;
import com.cygnus.ipoten.exception.GlobalExceptionHandler;
import com.cygnus.ipoten.google_authentication.exception.GoogleAccessTokenException;
import com.cygnus.ipoten.google_authentication.exception.GoogleGetUserInfoException;
import com.cygnus.ipoten.google_authentication.service.response.GoogleLoginResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class GoogleAuthenticationServiceImpl implements GoogleAuthenticationService {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenRequestUri;
    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;
    private final AccountProfileService accountProfileService;
    private final FrontendConfig frontendConfig;


    public GoogleAuthenticationServiceImpl(
            @Value("${google.client-id}") String clientId,
            @Value("${google.client-secret}") String clientSecret,
            @Value(" ${google.redirect-uri}") String redirectUri,
            @Value("${google.token-request-uri}") String tokenRequestUri,
            RestTemplate restTemplate,
            AuthenticationService authenticationService,
            AccountProfileService accountProfileService,
            FrontendConfig frontendConfig
    ) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.redirectUri = redirectUri;
            this.tokenRequestUri = tokenRequestUri;
            this.restTemplate = restTemplate;
            this.authenticationService = authenticationService;
            this.accountProfileService = accountProfileService;
            this.frontendConfig = frontendConfig;
    }




    @Override
    public String Link() {
        String scope = "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";

        return String.format(
                "https://accounts.google.com/o/oauth2/v2/auth?"
                        + "client_id=%s"
                        + "&redirect_uri=%s"
                        + "&response_type=code"
                        + "&scope=%s"
                        + "&access_type=offline"
                        + "&prompt=consent",
                clientId, redirectUri, scope
        );
    }


    @Override
    public GoogleLoginResponse handleLogin(String code) {
        String origin = frontendConfig.getOrigins().get(0);
        String accessToken = getAccessToken(code);
        Map<String, Object> userInfo = getUserInfo(accessToken);
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");

        Optional<AccountProfile> accountProfile = accountProfileService.loadProfileByEmail(email);

        boolean isNewUser = accountProfile.isEmpty();

        String token =
                isNewUser
                ? authenticationService.createTemporaryUserTokenWithAccessToken(accessToken)
                : authenticationService.createUserTokenWithAccessToken(accountProfile.get().getAccount().getId(), accessToken);

        return GoogleLoginResponse.of(isNewUser, token, name, email, origin);

    }

    @Override
    public String getAccessToken(String code) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenRequestUri, request, Map.class);

            Map<String, Object> body = response.getBody();
            return (body != null) ? (String) body.get("access_token") : null;

        } catch (RestClientException e) {
            throw new GoogleAccessTokenException("구글 로그인중 AccessTokenException : "+e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {
            throw new GoogleGetUserInfoException("구글 로그인중 UserInfoException : "+e.getMessage());
        }
    }


}
