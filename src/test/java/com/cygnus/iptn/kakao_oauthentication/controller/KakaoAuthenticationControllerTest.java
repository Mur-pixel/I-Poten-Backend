package com.cygnus.iptn.kakao_oauthentication.controller;


import com.cygnus.iptn.kakao_authentication.controller.KakaoAuthenticationController;
import com.cygnus.iptn.kakao_authentication.service.KakaoAuthenticationService;
import com.cygnus.iptn.kakao_authentication.service.response.ExistingUserKakaoLoginResponse;
import com.cygnus.iptn.kakao_authentication.service.response.KakaoLoginResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = KakaoAuthenticationController.class,
includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = KakaoAuthenticationController.class),
excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com.wowraid.jobspoon.quiz.*")
)
public class KakaoAuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KakaoAuthenticationService kakaoAuthenticationService;



    @Nested
    class login {


        @Test
        @DisplayName("카카오 로그인 성공시 기존 유저는 로그인 정보를 포함 HTML을 반환합니다")
        void 기존_유저_로그인_성공시_200_코드와_로그인_정보를_포함한_HTML을_반환합니다() throws Exception {
            // given
            String code = "test_code";
            String expectedNickname = "test_nickname";
            String expectedEmail = "test_email";
            String token = "test_token";
            String origin = "http://localhost:3000";
            boolean isNewUser = false;
            KakaoLoginResponse kakaoLoginResponse = new ExistingUserKakaoLoginResponse(isNewUser, token, expectedNickname, expectedEmail, origin);
            String html = kakaoLoginResponse.getHtmlResponse();

            given(kakaoAuthenticationService.handleLogin(code)).willReturn(kakaoLoginResponse);

            // when
            // then
            mockMvc.perform(get("/kakao-authentication/login").param("code", code))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/html;charset=UTF-8"))
                    .andExpect(content().string(html));


            verify(kakaoAuthenticationService).handleLogin(code);


        }




        @Test
        @DisplayName("카카오 로그인 실패시 500 코드를 반환합니다 ")
        void 카카오_로그인_실패시_카카오_로그인_실패_메세지와_500_에러_코드를_반환합니다() throws Exception {

            String code = "test_code";
            String token = "test_token";
            String origin = "http://localhost:3000";
            String expectedNickname = "test_nickname";
            String expectedEmail = "test_email";
            boolean isNewUser = false;
            KakaoLoginResponse kakaoLoginResponse = new ExistingUserKakaoLoginResponse(isNewUser, token, expectedNickname, expectedEmail, origin);
            String html = kakaoLoginResponse.getHtmlResponse();

            given(kakaoAuthenticationService.handleLogin(code)).willThrow(new RuntimeException("카카오 로그인 실패"));


            mockMvc.perform(get("/kakao-authentication/login").param("code", code))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("카카오 로그인 실패")));

            verify(kakaoAuthenticationService).handleLogin(code);

        }


    }






}
