package com.cygnus.ipoten.naver_authentication.controller;


import com.cygnus.ipoten.naver_authentication.service.NaverAuthenticationService;
import com.cygnus.ipoten.naver_authentication.service.response.NaverLoginResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/authentication/naver")
@RequiredArgsConstructor
public class NaverAuthenticationController {

    private final NaverAuthenticationService naverAuthenticationService;


    @GetMapping("/link")
    public String link() {
        return naverAuthenticationService.link();

    }

    @GetMapping("/login")
    public void login(@RequestParam("code") String code, HttpServletResponse response) throws IOException {

        try {
            NaverLoginResponse naverLoginResponse = naverAuthenticationService.handleLogin(code);
            if(!naverLoginResponse.getIsNewUser()){
                String cookieHeader = String.format(
                        "userToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict",
                        naverLoginResponse.getUserToken(),
//                    12 * 60 * 60
                        6 * 60 * 60// 6시간

                );        // CSRF 방어
                response.addHeader("Set-Cookie", cookieHeader);
            }

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(naverLoginResponse.getHtmlResponse());

        }catch (Exception e){
            log.error("Meta 로그인 에러", e);

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("Meta 로그인 실패: " + e.getMessage());

        }

    }
}
