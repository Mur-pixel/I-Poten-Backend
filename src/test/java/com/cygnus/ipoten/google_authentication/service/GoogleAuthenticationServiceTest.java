package com.cygnus.ipoten.google_authentication.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.naver_authentication.service.NaverAuthenticationService;
import com.cygnus.ipoten.naver_authentication.service.NaverAuthenticationServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class GoogleAuthenticationServiceTest {

    private GoogleAuthenticationServiceImpl googleAuthenticationService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AccountProfileService accountProfileService;

    private String clientId = "test-client-id";
    private String redirectUri = "http://localhost:8080/authentication/google/login";

    @BeforeEach
    public void setUp() {
        googleAuthenticationService = new GoogleAuthenticationServiceImpl(
                clientId,
                redirectUri
        );
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




}
