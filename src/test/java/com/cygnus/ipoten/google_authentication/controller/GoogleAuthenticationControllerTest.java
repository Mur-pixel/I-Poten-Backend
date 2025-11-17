package com.cygnus.ipoten.google_authentication.controller;

import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.google_authentication.service.GoogleAuthenticationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class GoogleAuthenticationControllerTest {

    @InjectMocks
    private GoogleAuthenticationController googleAuthenticationController;

    @Mock
    private GoogleAuthenticationService googleAuthenticationService;

    @Mock
    private AuthenticationService authenticationService;


    private String clientId = "test-client-id";
    private String redirectUri = "http://localhost:8080/authentication/google/login";


    @Test
    @DisplayName("모든_사용자는_구글_소셜_로그인_링크를_받을_수_있습니다.")
    public void 모든_사용자는_구글_소셜_로그인_링크를_받을_수_있습니다(){

        // given
        String googleLink = String.format("https://accounts.google.com/o/oauth2/v2/auth?"
                        + "client_id=%s"
                        + "&redirect_uri=%s"
                        + "&response_type=code",
                clientId, redirectUri);
        given(googleAuthenticationService.Link()).willReturn(googleLink);

        // when
        String resultLink = googleAuthenticationController.link();

        // then
        Assertions.assertEquals(googleLink, resultLink);

    }




}
