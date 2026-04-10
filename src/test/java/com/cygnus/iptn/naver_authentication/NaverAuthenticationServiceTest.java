package com.cygnus.iptn.naver_authentication;

import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.authentication.service.AuthenticationService;
import com.cygnus.iptn.authentication.service.AuthenticationServiceImpl;
import com.cygnus.iptn.config.FrontendConfig;
import com.cygnus.iptn.kakao_authentication.service.KakaoAuthenticationServiceImpl;
import com.cygnus.iptn.naver_authentication.service.NaverAuthenticationService;
import com.cygnus.iptn.naver_authentication.service.NaverAuthenticationServiceImpl;
import com.cygnus.iptn.naver_authentication.service.response.NaverLoginResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class NaverAuthenticationServiceTest {

    private NaverAuthenticationService naverAuthenticationService;

    @Mock
    private FrontendConfig frontendConfig;
    @Mock
    private AuthenticationService authService;
    @Mock
    private AccountProfileService accountProfileService;
    @Mock
    private AuthenticationServiceImpl authenticationService;
    @Mock
    private RestTemplate restTemplate;


    private String loginUrl = "https://oauth.naver.com";
    private String clientId = "test-client-id";
    private String clientSecret = "test-client-secret";
    private String redirectUri = "http://localhost:8080/authentication/naver/login";

    @BeforeEach
    public void setUp() {
        naverAuthenticationService = new NaverAuthenticationServiceImpl(
                loginUrl,
                clientId,
                clientSecret,
                redirectUri,
                restTemplate,
                frontendConfig,
                authService,
                accountProfileService

        ) {
        };
    }


    @Test
    @DisplayName("네이버_소셜_로그인_링크를_전달_받습니다")
    void 네이버_소셜_로그인_링크를_전달_받습니다() {

        // given
        // when

        String naverOAuthLink = naverAuthenticationService.link();

        String naverLink = String.format(
                "%s?client_id=%s&response_type=code&redirect_uri=%s&state=RANDOM_STRING",
                loginUrl, clientId, redirectUri
        );

        // then

        Assertions.assertEquals(naverOAuthLink, naverLink);

    }

    @Test
    @DisplayName("네이버_소셜_로그인_진행_성공")
    void 네이버_소셜_로그인_진행_시_신규_유저_임시_토큰_발행_성공(){

        // given
        Map<String, Object> testUserInfo = Map.of(
                "email", "testEmail",
                "nickName", "testNickname"
        );
        String testEamil = "testEamil";
        String temporaryCode = "testCode";
        String testAccessToken = "testAccessToken";
        String testTemporaryToken = "testTemporaryToken";

        given(accountProfileService.loadProfileByEmailAndLoginType(testEamil, LoginType.META)).willReturn(Optional.empty());
        given(authenticationService.createTemporaryUserTokenWithAccessToken(testAccessToken)).willReturn(testTemporaryToken);
        given(naverAuthenticationService.getAccessToken(temporaryCode)).willReturn(testTemporaryToken);
        given(naverAuthenticationService.getUserInfo(testAccessToken)).willReturn(testUserInfo);

        // when
        NaverLoginResponse naverLoginResponse = naverAuthenticationService.handleLogin(temporaryCode);


        // then
        Assertions.assertEquals(naverLoginResponse.getIsNewUser(), true);
        Assertions.assertEquals(naverLoginResponse.getUserToken(), testTemporaryToken);



    }
}
