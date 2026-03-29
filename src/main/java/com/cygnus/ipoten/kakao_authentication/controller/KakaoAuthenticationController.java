package com.cygnus.ipoten.kakao_authentication.controller;

import com.cygnus.ipoten.authentication.social.SocialLoginException;
import com.cygnus.ipoten.authentication.social.SocialLoginPopupResponseBuilder;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.common.util.CookieUtil;
import com.cygnus.ipoten.config.FrontendConfig;
import com.cygnus.ipoten.kakao_authentication.service.KakaoAuthenticationService;
import com.cygnus.ipoten.kakao_authentication.service.mobile_response.KakaoLoginMobileResponse;
import com.cygnus.ipoten.kakao_authentication.service.response.KakaoLoginResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/kakao-authentication")
@RequiredArgsConstructor
public class KakaoAuthenticationController {

    private final KakaoAuthenticationService kakaoAuthenticationService;
    private final AuthenticationService authenticationService;
    private final FrontendConfig frontendConfig;
    private final SocialLoginPopupResponseBuilder popupResponseBuilder;

    @GetMapping("/kakao/link")
    public String kakaoOauthLink() {
        return kakaoAuthenticationService.requestKakaoOauthLink();
    }

    @GetMapping("/login")
    public void kakaoLogin(@RequestParam("code") String code, HttpServletResponse response) throws Exception {
        try {
            KakaoLoginResponse kakaoLoginResponse = kakaoAuthenticationService.handleLogin(code);

            if (!kakaoLoginResponse.getIsNewUser()) {
                CookieUtil.addUserToken(response, kakaoLoginResponse.getUserToken());
                authenticationService.getAccountIdByUserToken(kakaoLoginResponse.getUserToken());
            }

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(kakaoLoginResponse.getHtmlResponse());
        } catch (SocialLoginException e) {
            response.setStatus(e.getStatus().value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(
                    popupResponseBuilder.buildErrorHtml(e.toResponse(), frontendConfig.getOrigins().get(0))
            );
        } catch (Exception e) {
            log.error("Kakao 로그인 에러", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("카카오 로그인 실패: " + e.getMessage());
        }
    }

    @GetMapping("/login/mobile")
    public ResponseEntity<?> kakaoLoginMobile(@RequestHeader("Authorization") String authenticationHeader) {
        String accessToken = authenticationHeader.replace("Bearer ", "").trim();

        try {
            KakaoLoginMobileResponse kakaoLoginMobileResponse = kakaoAuthenticationService.handleLoginMobile(accessToken);
            return new ResponseEntity<>(kakaoLoginMobileResponse, HttpStatus.OK);
        } catch (SocialLoginException e) {
            return ResponseEntity.status(e.getStatus()).body(e.toResponse());
        } catch (Exception e) {
            log.error("kakao 모바일 로그인 오류 발생", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
