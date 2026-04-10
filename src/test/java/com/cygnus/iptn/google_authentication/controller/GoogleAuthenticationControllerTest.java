//package com.cygnus.iptn.google_authentication.controller;
//
//import com.cygnus.iptn.accountProfile.service.AccountProfileService;
//import com.cygnus.iptn.authentication.service.AuthenticationService;
//import com.cygnus.iptn.google_authentication.exception.GoogleAccessTokenException;
//import com.cygnus.iptn.google_authentication.service.GoogleAuthenticationService;
//import com.cygnus.iptn.google_authentication.service.response.GoogleLoginResponse;
//import com.cygnus.iptn.google_authentication.service.response.NewUserGoogleLoginResponse;
//import jakarta.servlet.http.HttpServletResponse;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockHttpServletResponse;
//
//import java.io.IOException;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.BDDMockito.given;
//import static org.mockito.Mockito.mock;
//
//@ExtendWith(MockitoExtension.class)
//public class GoogleAuthenticationControllerTest {
//
//    @InjectMocks
//    private GoogleAuthenticationController googleAuthenticationController;
//
//    @Mock
//    private GoogleAuthenticationService googleAuthenticationService;
//
//    @Mock
//    private AuthenticationService authenticationService;
//
//    @Mock
//    private AccountProfileService accountProfileService;
//
//
//    private String clientId = "test-client-id";
//    private String redirectUri = "http://localhost:8080/authentication/google/login";
//
//
//    @Test
//    @DisplayName("모든_사용자는_구글_소셜_로그인_링크를_받을_수_있습니다.")
//    public void 모든_사용자는_구글_소셜_로그인_링크를_받을_수_있습니다(){
//
//        // given
//        String googleLink = String.format("https://accounts.google.com/o/oauth2/v2/auth?"
//                        + "client_id=%s"
//                        + "&redirect_uri=%s"
//                        + "&response_type=code",
//                clientId, redirectUri);
//        given(googleAuthenticationService.Link()).willReturn(googleLink);
//
//        // when
//        String resultLink = googleAuthenticationController.link();
//
//        // then
//        Assertions.assertEquals(googleLink, resultLink);
//
//    }
//
//
//    @Test
//    @DisplayName("구글_소셜_로그인_시_신규_회원은_임시토큰_발급_성공")
//    public void 구글_소셜_로그인_시_신규_회원은_임시토큰_발급_성공() throws IOException {
//
//        // given
//        GoogleLoginResponse mockResponse = new GoogleLoginResponse() {
//            @Override
//            public String getHtmlResponse() { return "<html>로그인 성공</html>"; }
//            @Override
//            public String getUserToken() { return "testToken"; }
//            @Override
//            public boolean isNewUser() { return false; }
//        };
//
//        given(googleAuthenticationService.handleLogin("testCode"))
//                .willReturn(mockResponse);
//        HttpServletResponse response = new MockHttpServletResponse();
//        String testCode = "testCode";
//        String testToken = "testToken";
//        String temporaryToken = "temporaryToken";
//        String testEmail = "testEmail";
//        given(authenticationService.createTemporaryUserTokenWithAccessToken(testToken)).willReturn(temporaryToken);
//        given(accountProfileService.loadProfileByEmail(testEmail)).willReturn(Optional.empty());
//
//
//
//        // when & then
//        assertDoesNotThrow(() -> {
//            googleAuthenticationController.login(testCode, response);
//        });
//
//
//    }
//
//    @Test
//    @DisplayName("구글_소셜_로그인_시_신규_회원은_임시토큰_발급_실패")
//    public void 구글_소셜_로그인_시_신규_회원은_임시토큰_발급_실패() throws IOException {
//
//        // given
//        HttpServletResponse response = new MockHttpServletResponse();
//        String testCode = "testCode";
//        String testToken = "testToken";
//        String temporaryToken = "temporaryToken";
//        given(authenticationService.createTemporaryUserTokenWithAccessToken(testToken))
//                .willThrow(new GoogleAccessTokenException("액세스토큰 발급 실패"));
//
//        // when
//        // when & then
//        assertThrows(GoogleAccessTokenException.class, () -> {
//            googleAuthenticationController.login(testCode, response);
//        });
//
//
//    }
//
//
//
//}
//
