package com.cygnus.ipoten.google_authentication.controller;

import com.cygnus.ipoten.google_authentication.service.GoogleAuthenticationService;
import com.cygnus.ipoten.google_authentication.service.response.GoogleLoginResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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
                String cookieHeader = String.format(
                        "userToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict",
                        googleLoginResponse.getUserToken(),
                        6 * 60 * 60
                );
                response.addHeader("Set-Cookie", cookieHeader);

            }

            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write(googleLoginResponse.getHtmlResponse());

        } catch (Exception e) {
            log.error("google로그인 에러 : {}", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("google 로그인 실패: " + e.getMessage());
        }

    }

}
