package com.cygnus.ipoten.naver_authentication;

import com.cygnus.ipoten.naver_authentication.controller.NaverAuthenticationController;
import com.cygnus.ipoten.naver_authentication.service.NaverAuthenticationService;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class NaverAuthenticationControllerTest {

    @Mock
    private NaverAuthenticationService naverAuthenticationService;

    @InjectMocks
    private NaverAuthenticationController naverAuthenticationController;

    private String loginUrl = "https://oauth.naver.com";
    private String clientId = "test-client-id";
    private String clientSecret = "test-client-secret";
    private String redirectUri = "http://localhost:8080/authentication/naver/login";


    @Test
    @DisplayName("네이버_소셜_로그인_url을_요청하는_엔드포인트에_요청하여_소셜_로그인_링크_응답")
    void 네이버_소셜_로그인_url을_요청하는_엔드포인트에_요청하여_소셜_로그인_링크_응답_성공(){

        // given
        String naverLink = String.format(
                "%s?client_id=%s&response_type=code&redirect_uri=%s&state=RANDOM_STRING",
                loginUrl, clientId, redirectUri
        );
        given(naverAuthenticationService.link()).willReturn(naverLink);

        // when
        String resultLink = naverAuthenticationController.link();

        // then
        Assertions.assertEquals(naverLink, resultLink);


    }




}
