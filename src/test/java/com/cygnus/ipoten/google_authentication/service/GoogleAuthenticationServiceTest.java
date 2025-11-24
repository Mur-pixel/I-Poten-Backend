package com.cygnus.ipoten.google_authentication.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.config.FrontendConfig;
import com.cygnus.ipoten.exception.GlobalExceptionHandler;
import com.cygnus.ipoten.google_authentication.exception.GoogleAccessTokenException;
import com.cygnus.ipoten.google_authentication.service.response.GoogleLoginResponse;
import com.cygnus.ipoten.google_authentication.service.response.NewUserGoogleLoginResponse;
import com.cygnus.ipoten.naver_authentication.service.NaverAuthenticationService;
import com.cygnus.ipoten.naver_authentication.service.NaverAuthenticationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class GoogleAuthenticationServiceTest {

    private GoogleAuthenticationServiceImpl googleAuthenticationService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AccountProfileService accountProfileService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FrontendConfig frontendConfig;



    private String clientId = "test-client-id";
    private String redirectUri = "http://localhost:8080/authentication/google/login";
    private String clientSecret = "http://localhost:8080/authentication/google/login";
    private String tokenRequestUri = "http://localhost:8080/authentication/google/login";


    @BeforeEach
    public void setUp() {
        googleAuthenticationService = Mockito.spy(new GoogleAuthenticationServiceImpl(
                clientId,
                clientSecret,
                redirectUri,
                tokenRequestUri,
                restTemplate,
                authenticationService,
                accountProfileService,
                frontendConfig
        ));
    }



    @Test
    @DisplayName("구글_소셜_로그인_링크_제공에_성공")
    public void 구글_소셜_로그인_링크_제공에_성공(){

        // given
        String googleLink = String.format("https://accounts.google.com/o/oauth2/v2/auth?"
                        + "client_id=%s"
                        + "&redirect_uri=%s"
                        + "&response_type=code",
                clientId, redirectUri);


        // when
        String link = googleAuthenticationService.Link();


        // then
        Assertions.assertEquals(googleLink, link);

    }

//    @Test
//    @DisplayName("구글_소셜_로그인_시_신규_회원_구분_성공")
//    public void 구글_소셜_로그인_시_신규_회원_구분_성공() {
//
//        // given
//        String testCode = "testCode";
//        String temporaryToken = "temporaryToken";
//        String testEmail = "testEmail";
//        Account testAccount = new Account();
//
//        given(frontendConfig.getOrigins()).willReturn(List.of("http://localhost:8080"));
//
//        doReturn(temporaryToken).when(googleAuthenticationService).getAccessToken(testCode);
//        doReturn(Map.of("email", testEmail, "name", "Test Name"))
//                .when(googleAuthenticationService).getUserInfo(temporaryToken);
//        doReturn(Optional.of(testAccount))
//                .when(accountProfileService).loadProfileByEmail(testEmail);
//        given(authenticationService.createTemporaryUserTokenWithAccessToken(anyString()))
//                .willReturn(temporaryToken);
//
//
//
//
//        // when
//        GoogleLoginResponse googleLoginResponse = googleAuthenticationService.handleLogin(testCode);
//
//        // then
//        Assertions.assertTrue(googleLoginResponse.isNewUser());
//        Assertions.assertEquals(temporaryToken, googleLoginResponse.getUserToken());
//    }


    @Test
    @DisplayName("구글_소셜_로그인_시_신규_회원_구분_실패")
    public void 구글_소셜_로그인_시_신규_회원_구분_실패() {

        // given
        String testCode = "testCode";
        String temporaryToken = "temporaryToken";
        String testEmail = "testEmail";
        String testName = "testName";

        given(frontendConfig.getOrigins()).willReturn(List.of("http://localhost:8080"));
        given(accountProfileService.loadProfileByEmail(testEmail)).willReturn(Optional.empty());
        given(authenticationService.createTemporaryUserTokenWithAccessToken(temporaryToken)).willReturn(temporaryToken);

        // Spy로 만든 서비스에서 특정 메소드만 mocking
        doReturn(temporaryToken).when(googleAuthenticationService).getAccessToken(testCode);
        doReturn(Map.of("email", testEmail, "name", testName)).when(googleAuthenticationService).getUserInfo(temporaryToken);

        // when
        GoogleLoginResponse googleLoginResponse = googleAuthenticationService.handleLogin(testCode);

        // then
        Assertions.assertTrue(googleLoginResponse.isNewUser()); // 실패 케이스는 Optional.empty() → 새로운 유저
        Assertions.assertEquals(temporaryToken, googleLoginResponse.getUserToken());
    }


    @Test
    @DisplayName("구글_소셜_로그인_시_신규_회원은_임시토큰_발급_성공")
    public void 구글_소셜_로그인_시_신규_회원은_임시토큰_발급_성공(){
        // given
        String testCode = "testCode";
        String testToken = "testToken";
        String temporaryToken = "temporaryToken";
        String testEmail = "testEmail";

        given(frontendConfig.getOrigins()).willReturn(List.of("http://localhost:8080"));
        doReturn(testToken).when(googleAuthenticationService).getAccessToken(testCode);
        doReturn(Map.of("email", testEmail, "name", "testName"))
                .when(googleAuthenticationService).getUserInfo(testToken);
        given(authenticationService.createTemporaryUserTokenWithAccessToken(testToken)).willReturn(temporaryToken);
        doReturn(Optional.empty()).when(accountProfileService).loadProfileByEmail(testEmail);

        // when
        GoogleLoginResponse googleLoginResponse = googleAuthenticationService.handleLogin(testCode);

        // then
        Assertions.assertEquals(temporaryToken, googleLoginResponse.getUserToken());
        Assertions.assertTrue(googleLoginResponse.isNewUser());
    }


    @Test
    @DisplayName("구글_소셜_로그인_액세스_토큰_발급_실패")
    public void 구글_소셜_로그인_액세스_토큰_발급_실패(){

        // given
        String testCode = "testCode";
        String testToken = "testToken";
        String temporaryToken = "temporaryToken";
        given(frontendConfig.getOrigins()).willReturn(List.of("http://localhost:8080"));

        // when
        // then
        Assertions.assertThrows(GoogleAccessTokenException.class,
                () -> googleAuthenticationService.handleLogin(testCode));


    }


}

