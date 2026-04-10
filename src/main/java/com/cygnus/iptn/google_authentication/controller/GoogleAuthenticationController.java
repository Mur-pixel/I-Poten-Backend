package com.cygnus.iptn.google_authentication.controller;

import com.cygnus.iptn.authentication.social.SocialLoginException;
import com.cygnus.iptn.authentication.social.SocialLoginPopupResponseBuilder;
import com.cygnus.iptn.common.util.CookieUtil;
import com.cygnus.iptn.config.FrontendConfig;
import com.cygnus.iptn.google_authentication.service.GoogleAuthenticationService;
import com.cygnus.iptn.google_authentication.service.mobile_response.GoogleLoginMobileResponse;
import com.cygnus.iptn.google_authentication.service.response.GoogleLoginResponse;
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

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/authentication/google")
public class GoogleAuthenticationController {

    private final GoogleAuthenticationService googleAuthenticationService;
    private final FrontendConfig frontendConfig;
    private final SocialLoginPopupResponseBuilder popupResponseBuilder;

    @GetMapping("/link")
    public String link() {
        return googleAuthenticationService.Link();
    }

    @GetMapping("/login")
    public void login(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {
        try {
            GoogleLoginResponse googleLoginResponse = googleAuthenticationService.handleLogin(code);
            if (!googleLoginResponse.isNewUser()) {
                CookieUtil.addUserToken(response, googleLoginResponse.getUserToken());
            }

            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write(googleLoginResponse.getHtmlResponse());
        } catch (SocialLoginException e) {
            response.setStatus(e.getStatus().value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(
                    popupResponseBuilder.buildErrorHtml(e.toResponse(), frontendConfig.getOrigins().get(0))
            );
        } catch (Exception e) {
            log.error("google로그인 에러 : {}", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("google 로그인 실패: " + e.getMessage());
        }
    }

    @GetMapping("/login/mobile")
    public ResponseEntity<?> loginMobile(@RequestHeader("Authorization") String authenticationHeader) {
        String accessToken = authenticationHeader.replace("Bearer ", "").trim();

        try {
            GoogleLoginMobileResponse googleLoginMobileResponse = googleAuthenticationService.handleLoginMobile(accessToken);
            return new ResponseEntity<>(googleLoginMobileResponse, HttpStatus.OK);
        } catch (SocialLoginException e) {
            return ResponseEntity.status(e.getStatus()).body(e.toResponse());
        } catch (Exception e) {
            log.error("google 모바일 로그인 오류 발생", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
