package com.cygnus.iptn.kakao_oauthentication.service;


import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.accountProfile.entity.AccountProfile;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import com.cygnus.iptn.config.FrontendConfig;
import com.cygnus.iptn.kakao_authentication.service.KakaoAuthenticationServiceImpl;
import com.cygnus.iptn.kakao_authentication.service.response.ExistingUserKakaoLoginResponse;
import com.cygnus.iptn.kakao_authentication.service.response.KakaoLoginResponse;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class KakaoAuthenticationServiceTest {

    @Mock
    RestTemplate restTemplate;

    @Mock
    AccountProfileService accountProfileService;

    @Mock
    FrontendConfig frontendConfig;

    @Mock
    RedisCacheService redisCacheService;

    @Mock
    AuthenticationService authenticationService;


    private KakaoAuthenticationServiceImpl kakaoAuthenticationService;

    private String loginUrl = "https://kauth.kakao.com";
    private String clientId = "test-client-id";
    private String redirectUri = "http://localhost:8080/oauth/kakao/callback";
    private final String TEST_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private final String TEST_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";



    @BeforeEach
    void setUp() {
        kakaoAuthenticationService = new KakaoAuthenticationServiceImpl(
                loginUrl,
                clientId,
                redirectUri,
                TEST_TOKEN_URL,
                TEST_USER_INFO_URL,
                restTemplate,
                accountProfileService,
                frontendConfig,
                authenticationService
        );
    }


    @Test
    @DisplayName("로그인_요청_시_KakaoOauth_로그인_url_응답")
    void 로그인_요청_시_KakaoOauth_로그인_url_응답() {

        // given
        String oauthLink = kakaoAuthenticationService.requestKakaoOauthLink();

        // when
        String link = String.format("%s/oauth/authorize?client_id=%s&redirect_uri=%s&response_type=code",
                loginUrl, clientId, redirectUri);
        // then
        Assertions.assertEquals(oauthLink, link);




    }



    @Test
    void loginUrl이_null일_경우_IllegalStateException_발생() {
        // given
        ReflectionTestUtils.setField(kakaoAuthenticationService, "loginUrl", null);
        ReflectionTestUtils.setField(kakaoAuthenticationService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(kakaoAuthenticationService, "redirectUri", "http://localhost:8080/callback");

        // when & then
        Assertions.assertThrows(IllegalStateException.class, () -> {
            kakaoAuthenticationService.requestKakaoOauthLink();
        });
    }

    @Test
    void clientId가_null일_경우_IllegalStateException_발생() {
        // given
        ReflectionTestUtils.setField(kakaoAuthenticationService, "loginUrl", "https://kauth.kakao.com");
        ReflectionTestUtils.setField(kakaoAuthenticationService, "clientId", null);
        ReflectionTestUtils.setField(kakaoAuthenticationService, "redirectUri", "http://localhost:8080/callback");

        // when & then
        Assertions.assertThrows(IllegalStateException.class, () -> {
            kakaoAuthenticationService.requestKakaoOauthLink();
        });
    }

    @Test
    void redirectUri가_null일_경우_IllegalStateException_발생() {
        // given
        ReflectionTestUtils.setField(kakaoAuthenticationService, "loginUrl", "https://kauth.kakao.com");
        ReflectionTestUtils.setField(kakaoAuthenticationService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(kakaoAuthenticationService, "redirectUri", null);

        // when & then
        Assertions.assertThrows(IllegalStateException.class, () -> {
            kakaoAuthenticationService.requestKakaoOauthLink();
        });
    }


    @Test
    void 유효한_코드일_때_AccessToken을_반환헙니다(){

        // given
        String testCode = "test_code";
        String expectedToken = "test_access_token";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", expectedToken);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(TEST_TOKEN_URL),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // when
        Map<String, Object> actualResponse = kakaoAuthenticationService.getAccessToken(testCode);

        // then
        Assertions.assertNotNull(actualResponse);
        Assertions.assertEquals(expectedToken,actualResponse.get("access_token"));



    }


    @Test
    void 유효한_AccessToken일_때_유저_정보를_반환합니다(){

        // given
        String testAccessToken = "test_access_token";
        String expectedEmail = "test_expected_email";
        String expectedNickname = "test_expected_nickname";

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("kakao_account", Map.of("email", expectedEmail, "nickname", expectedNickname));
        ResponseEntity<Map> ResponseEntity = new ResponseEntity<>(userInfo, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(TEST_USER_INFO_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity);

        // when
        Map<String, Object> actualUserInfo = kakaoAuthenticationService.getUserInfo(testAccessToken);

        // then
        Assertions.assertNotNull(actualUserInfo);
        Map<String, Object> kakaoAccount = (Map<String, Object>) actualUserInfo.get("kakao_account");
        Assertions.assertEquals(expectedEmail, kakaoAccount.get("email"));
        Assertions.assertEquals(expectedNickname, kakaoAccount.get("nickname"));

    }

    @Test
    void 카카오_이메일_정보_가져오기_실패(){

        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", null);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("properties", properties);
//        when(kakaoAuthenticationService.extractNickname(userInfo)).thenReturn(null);

        // when
        // then
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            kakaoAuthenticationService.extractNickname(userInfo);
        });

    }


    @Test
    void 카카오_닉네임_정보_가져오기_실패(){

        // given
        Map<String, Object> kakao_account = new HashMap<>();
        kakao_account.put("email", null);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("kakao_account", kakao_account);
//        when(kakaoAuthenticationService.extractEmail(userInfo)).thenReturn(null);

        // when
        // then
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            kakaoAuthenticationService.extractEmail(userInfo);
        });

    }

    @Test
    void 카카오_이메일_정보_가져오기_성공(){

        // given
        Map<String, Object> properties = new HashMap<>();
        properties.put("nickname", "test_nickname");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("properties", properties);
//        when(kakaoAuthenticationService.extractNickname(userInfo)).thenReturn(null);

        // when
        // then
        Assertions.assertDoesNotThrow(() -> {
            kakaoAuthenticationService.extractNickname(userInfo);
        });

    }


    @Test
    void 카카오_닉네임_정보_가져오기_성공(){

        // given
        Map<String, Object> kakao_account = new HashMap<>();
        kakao_account.put("email", "test_email");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("kakao_account", kakao_account);
//        when(kakaoAuthenticationService.extractEmail(userInfo)).thenReturn(null);

        // when
        // then
        Assertions.assertDoesNotThrow(() -> {
            kakaoAuthenticationService.extractEmail(userInfo);
        });

    }


    @Test
    void 카카오에서_받은_정보에서_이메일_추출_성공(){

        // given
        String expectedEmail = "test_email";
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("kakao_account", Map.of("email", expectedEmail));


        // when
        String email = kakaoAuthenticationService.extractEmail(userInfo);

        // then

    }

    @Test
    void 카카오에서_받은_정보에서_닉네임_추출_성공(){

        // given
        String expectedNickname = "test_nickname";
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("properties", Map.of("nickname", expectedNickname));

        // when
        String nickname = kakaoAuthenticationService.extractNickname(userInfo);

        // then

    }

    @Test
    void 카카오에서_받은_정보에서_이메일_추출_실패(){

        // given
        String expectedEmail = "test_email";
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("kakao_account", Map.of("email", expectedEmail));


        // when
        String email = kakaoAuthenticationService.extractEmail(userInfo);

        // then

    }

    @Test
    void 카카오에서_받은_정보에서_닉네임_추출_실패(){

        // given
        String expectedNickname = "test_nickname";
        // when

        // then

    }



    @Test
    void 회원이_기존_회원인지_판단합니다(){

        // given
        String testCode = "test_code";
        String expectedToken = "test_access_token";
        String expectedEmail = "test_expected_email";
        String expectedNickname = "test_expected_nickname";

        Account account = new Account(1L);
        AccountProfile accountProfile = new AccountProfile(account, expectedNickname, expectedEmail);

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", expectedToken);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(TEST_TOKEN_URL),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);


        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("kakao_account", Map.of("email", expectedEmail));
        userInfo.put("properties",Map.of( "nickname", expectedNickname));
        ResponseEntity<Map> ResponseEntity = new ResponseEntity<>(userInfo, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(TEST_USER_INFO_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity);

        when(accountProfileService.loadProfileByEmailAndLoginType(expectedEmail, LoginType.KAKAO)).thenReturn(Optional.of(accountProfile));


        // when
        KakaoLoginResponse kakaoLoginResponse = kakaoAuthenticationService.handleLogin(testCode);

        // then
        Assertions.assertNotNull(kakaoLoginResponse);
        Assertions.assertTrue(kakaoLoginResponse instanceof ExistingUserKakaoLoginResponse);




    }








}
