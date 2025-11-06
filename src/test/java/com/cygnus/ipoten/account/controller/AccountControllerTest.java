//package com.wowraid.jobspoon.account.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.wowraid.jobspoon.account.controller.request_form.RegisterRequestForm;
//import com.wowraid.jobspoon.account.service.SignupServiceImpl;
//import com.wowraid.jobspoon.account.service.register_response.RegisterResponse;
//import com.wowraid.jobspoon.account.entity.LoginType;
//import com.wowraid.jobspoon.redis_cache.RedisCacheService;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@ExtendWith(MockitoExtension.class)
//public class AccountControllerTest {
//
//    @InjectMocks
//    private AccountController accountController;
//
//    @Mock
//    private SignupServiceImpl signupService;
//
//    @Mock
//    private RedisCacheService redisCacheService;
//
//
//    @Nested
//    @DisplayName("회원가입 기능")
//    class SignupToController {
//
//        @Test
//        void 회원가입_요청시_회원가입_성공() {
//            // given
//            String email = "email@test.com";
//            String nickname = "tester";
//            String accessToken = "accessToken";
//            String tempToken = "tempToken";
//
//            RegisterRequestForm form =
//                    new RegisterRequestForm(email, nickname, LoginType.KAKAO);
//            RegisterResponse response =
//                    new RegisterResponse(nickname, email, accessToken);
//
//            given(signupService.signup("Bearer " + tempToken, form)).willReturn(response);
//
//            // when
//            ResponseEntity<RegisterResponse> result =
//                    accountController.signup("Bearer " + tempToken, form);
//
//            // then
//            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//            Assertions.assertNotNull(result.getBody());
//            assertThat(result.getBody().getEmail()).isEqualTo(email);
//            assertThat(result.getBody().getNickname()).isEqualTo(nickname);
//        }
//    }
//}
