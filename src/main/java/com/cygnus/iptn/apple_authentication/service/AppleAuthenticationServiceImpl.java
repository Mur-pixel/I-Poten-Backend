package com.cygnus.iptn.apple_authentication.service;

import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.apple_authentication.controller.request.AppleLoginMobileRequest;
import com.cygnus.iptn.apple_authentication.service.mobile_response.AppleLoginMobileResponse;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import com.cygnus.iptn.authentication.social.SocialLoginPolicyService;
import com.cygnus.iptn.mobile_auth.service.RefreshTokenService;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
public class AppleAuthenticationServiceImpl implements AppleAuthenticationService {

    private final String clientId;
    private final String iosBundleId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenRequestUri;
    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;
    private final AccountProfileService accountProfileService;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper;
    private final SocialLoginPolicyService socialLoginPolicyService;

    public AppleAuthenticationServiceImpl(
            @Value("${apple.client-id}") String clientId,
            @Value("${apple.ios-bundle-id}") String iosBundleId,
            @Value("${apple.client-secret}") String clientSecret,
            @Value("${apple.redirect-uri}") String redirectUri,
            @Value("${apple.token-request-uri}") String tokenRequestUri,
            RestTemplate restTemplate,
            AuthenticationService authenticationService,
            AccountProfileService accountProfileService,
            RefreshTokenService refreshTokenService,
            ObjectMapper objectMapper,
            SocialLoginPolicyService socialLoginPolicyService
    ) {
        this.clientId = clientId;
        this.iosBundleId = iosBundleId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.tokenRequestUri = tokenRequestUri;
        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
        this.accountProfileService = accountProfileService;
        this.refreshTokenService = refreshTokenService;
        this.objectMapper = objectMapper;
        this.socialLoginPolicyService = socialLoginPolicyService;
    }

    @Override
    public AppleLoginMobileResponse handleLoginMobile(AppleLoginMobileRequest request) {
        boolean isIos = "ios".equalsIgnoreCase(request.getPlatform());

        log.info("Apple mobile login start - platform: {}, clientId: {}, codeLength: {}",
                request.getPlatform(),
                isIos ? iosBundleId : clientId,
                request.getAuthorizationCode() == null ? 0 : request.getAuthorizationCode().length());

        String accessToken;
        String idToken;

        if (isIos) {
            idToken = request.getIdentityToken();
            if (isBlank(idToken)) {
                throw new IllegalArgumentException("애플 identityToken이 비어 있습니다.");
            }
            accessToken = request.getAuthorizationCode();
        } else {
            Map<String, Object> tokenResponse = exchangeAuthorizationCode(request.getAuthorizationCode(), request.getPlatform());
            accessToken = asString(tokenResponse.get("access_token"));
            if (isBlank(accessToken)) {
                throw new IllegalArgumentException("애플 액세스 토큰이 비어 있습니다.");
            }

            String responseIdToken = asString(tokenResponse.get("id_token"));
            idToken = firstNonBlank(responseIdToken, request.getIdentityToken());
        }

        Map<String, Object> claims = decodeIdTokenClaims(idToken);
        String tokenEmail = asString(claims.get("email"));
        String requestEmail = request.getEmail();
        String email = firstNonBlank(tokenEmail, requestEmail);
        log.info(
                "Apple login email resolved - platform: {}, tokenEmail: {}, requestEmail: {}, resolvedEmail: {}, hasIdentityToken: {}, authCodeLength: {}",
                request.getPlatform(),
                maskEmail(tokenEmail),
                maskEmail(requestEmail),
                maskEmail(email),
                !isBlank(idToken),
                request.getAuthorizationCode() == null ? 0 : request.getAuthorizationCode().length()
        );
        if (isBlank(email)) {
            throw new IllegalArgumentException("애플 이메일을 확인할 수 없습니다.");
        }

        var loginResult = socialLoginPolicyService.login(email, LoginType.APPLE, accessToken);
        String nickname = buildNickname(request.getGivenName(), request.getFamilyName(), email);

        if (loginResult.isNewUser()) {
            return new AppleLoginMobileResponse(true, loginResult.token(), nickname, email, loginResult.isRejoinUser());
        }

        var account = loginResult.account();
        String refreshToken = refreshTokenService.createOrReplace(account);
        return new AppleLoginMobileResponse(false, loginResult.token(), nickname, email, refreshToken);
    }

    private Map<String, Object> exchangeAuthorizationCode(String authorizationCode, String platform) {
        if (isBlank(authorizationCode)) {
            throw new IllegalArgumentException("애플 authorizationCode가 비어 있습니다.");
        }
        if (isBlank(clientSecret)) {
            throw new IllegalStateException("APPLE_CLIENT_SECRET 환경변수가 비어 있습니다.");
        }

        boolean isIos = "ios".equalsIgnoreCase(platform);
        String effectiveClientId = isIos ? iosBundleId : clientId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", effectiveClientId);
            params.add("client_secret", clientSecret);
            params.add("code", authorizationCode);
            params.add("grant_type", "authorization_code");
            if (!isIos) {
                params.add("redirect_uri", redirectUri);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenRequestUri, request, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("애플 토큰 응답이 비어 있습니다.");
            }
            return body;
        } catch (HttpStatusCodeException e) {
            log.error("Apple token exchange failed - status: {}, body: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("애플 토큰 교환 실패: " + e.getMessage(), e);
        } catch (RestClientException e) {
            log.error("Apple token exchange failed - clientId: {}, redirectUri: {}, message: {}",
                    clientId,
                    redirectUri,
                    e.getMessage(),
                    e);
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

    private String maskEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
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
