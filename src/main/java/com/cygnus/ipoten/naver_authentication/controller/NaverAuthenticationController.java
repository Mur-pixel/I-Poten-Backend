package com.cygnus.ipoten.naver_authentication.controller;

import com.cygnus.ipoten.authentication.social.SocialLoginException;
import com.cygnus.ipoten.authentication.social.SocialLoginPopupResponseBuilder;
import com.cygnus.ipoten.common.util.CookieUtil;
import com.cygnus.ipoten.config.FrontendConfig;
import com.cygnus.ipoten.naver_authentication.service.NaverAuthenticationService;
import com.cygnus.ipoten.naver_authentication.service.mobile_response.NaverLoginMobileResponse;
import com.cygnus.ipoten.naver_authentication.service.response.NaverLoginResponse;
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
@RequestMapping("/authentication/naver")
@RequiredArgsConstructor
public class NaverAuthenticationController {

    private final NaverAuthenticationService naverAuthenticationService;
    private final FrontendConfig frontendConfig;
    private final SocialLoginPopupResponseBuilder popupResponseBuilder;

    @GetMapping("/link")
    public String link() {
        return naverAuthenticationService.link();
    }

    @GetMapping("/login")
    public void login(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        try {
            NaverLoginResponse naverLoginResponse = naverAuthenticationService.handleLogin(code);
            if (!naverLoginResponse.getIsNewUser()) {
                CookieUtil.addUserToken(response, naverLoginResponse.getUserToken());
            }

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(naverLoginResponse.getHtmlResponse());
        } catch (SocialLoginException e) {
            response.setStatus(e.getStatus().value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(
                    popupResponseBuilder.buildErrorHtml(e.toResponse(), frontendConfig.getOrigins().get(0))
            );
        } catch (Exception e) {
            log.error("naver login error", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("Naver 로그인 실패: " + e.getMessage());
        }
    }

    @GetMapping("/login/mobile")
    public ResponseEntity<?> loginMobile(@RequestHeader("Authorization") String authenticationHeader) {
        String accessToken = authenticationHeader.replace("Bearer ", "").trim();

        try {
            NaverLoginMobileResponse naverLoginMobileResponse =
                    naverAuthenticationService.handleLoginMobile(accessToken);
            return new ResponseEntity<>(naverLoginMobileResponse, HttpStatus.OK);
        } catch (SocialLoginException e) {
            return ResponseEntity.status(e.getStatus()).body(e.toResponse());
        } catch (Exception e) {
            log.info("네이버 모바일 로그인 오류 발생 : {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
