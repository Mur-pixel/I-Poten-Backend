package com.cygnus.ipoten.apple_authentication.service;

import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.apple_authentication.controller.request.AppleLoginMobileRequest;
import com.cygnus.ipoten.apple_authentication.service.mobile_response.AppleLoginMobileResponse;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.mobile_auth.service.RefreshTokenService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AppleAuthenticationServiceImpl implements AppleAuthenticationService {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenRequestUri;
    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;
    private final AccountProfileService accountProfileService;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;

    public AppleAuthenticationServiceImpl(
            @Value("${apple.client-id}") String clientId,
            @Value("${apple.client-secret}") String clientSecret,
            @Value("${apple.redirect-uri}") String redirectUri,
            @Value("${apple.token-request-uri}") String tokenRequestUri,
            RestTemplate restTemplate,
            AuthenticationService authenticationService,
            AccountProfileService accountProfileService,
            RefreshTokenService refreshTokenService,
            ObjectMapper objectMapper
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.tokenRequestUri = tokenRequestUri;
        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
        this.accountProfileService = accountProfileService;
        this.refreshTokenService = refreshTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AppleLoginMobileResponse handleLoginMobile(AppleLoginMobileRequest request) {
        Map<String, Object> tokenResponse = exchangeAuthorizationCode(request.getAuthorizationCode());
        String accessToken = asString(tokenResponse.get("access_token"));
        if (isBlank(accessToken)) {
            throw new IllegalArgumentException("애플 액세스 토큰이 비어 있습니다.");
        }

        String responseIdToken = asString(tokenResponse.get("id_token"));
        String idToken = firstNonBlank(responseIdToken, request.getIdentityToken());
        Map<String, Object> claims = decodeIdTokenClaims(idToken);

        String email = firstNonBlank(asString(claims.get("email")), request.getEmail());
        if (isBlank(email)) {
            throw new IllegalArgumentException("애플 이메일을 확인할 수 없습니다.");
        }

        Optional<AccountProfile> accountProfile = accountProfileService.loadProfileByEmailAndLoginType(email, LoginType.APPLE);
        boolean isNewUser = accountProfile.isEmpty();
        String nickname = buildNickname(request.getGivenName(), request.getFamilyName(), email);

        if (isNewUser) {
            String tempToken = authenticationService.createTemporaryUserTokenWithAccessToken(accessToken);
            return new AppleLoginMobileResponse(true, tempToken, nickname, email);
        }

        AccountProfile profile = accountProfile.get();
        String userToken = authenticationService.createUserTokenWithAccessToken(profile.getAccount().getId(), accessToken);
        String refreshToken = refreshTokenService.createOrReplace(profile.getAccount());
        return new AppleLoginMobileResponse(false, userToken, profile.getNickname(), email, refreshToken);
    }

    private Map<String, Object> exchangeAuthorizationCode(String authorizationCode) {
        if (isBlank(authorizationCode)) {
            throw new IllegalArgumentException("애플 authorizationCode가 비어 있습니다.");
        }
        if (isBlank(clientSecret)) {
            throw new IllegalStateException("APPLE_CLIENT_SECRET 환경변수가 비어 있습니다.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("code", authorizationCode);
            params.add("grant_type", "authorization_code");
            params.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenRequestUri, request, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("애플 토큰 응답이 비어 있습니다.");
            }
            return body;
        } catch (RestClientException e) {
            throw new RuntimeException("애플 토큰 교환 실패: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> decodeIdTokenClaims(String idToken) {
        if (isBlank(idToken)) {
            return Collections.emptyMap();
        }

        try {
            String[] chunks = idToken.split("\\.");
            if (chunks.length < 2) {
                return Collections.emptyMap();
            }
            byte[] decoded = Base64.getUrlDecoder().decode(padBase64(chunks[1]));
            String payload = new String(decoded, StandardCharsets.UTF_8);
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("애플 id_token 파싱 실패: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String buildNickname(String givenName, String familyName, String email) {
        String fullName = firstNonBlank(
                concatName(familyName, givenName),
                concatName(givenName, familyName),
                email != null && email.contains("@") ? email.substring(0, email.indexOf('@')) : email
        );
        return fullName == null ? "Apple User" : fullName;
    }

    private String concatName(String first, String second) {
        if (isBlank(first) && isBlank(second)) {
            return null;
        }
        if (isBlank(first)) {
            return second.trim();
        }
        if (isBlank(second)) {
            return first.trim();
        }
        return (first + second).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String padBase64(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        return value + "=".repeat(4 - remainder);
    }
}
