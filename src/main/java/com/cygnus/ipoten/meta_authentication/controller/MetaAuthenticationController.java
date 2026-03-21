package com.cygnus.ipoten.meta_authentication.controller;

import com.cygnus.ipoten.common.util.CookieUtil;
import com.cygnus.ipoten.meta_authentication.service.MetaAuthenticationService;
import com.cygnus.ipoten.meta_authentication.service.response.MetaLoginResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/meta_authentication")
public class MetaAuthenticationController {

    private final MetaAuthenticationService metaAuthenticationService;


    @GetMapping("/link")
    public String metaOauthLink(){
        return metaAuthenticationService.requestKakaoOauthLink();
    }


    @GetMapping("/login")
    public void login(@RequestParam("code") String code, HttpServletResponse response) throws Exception {
        log.info("Meta Login 접근");

        try {
            MetaLoginResponse metaLoginResponse = metaAuthenticationService.handleLogin(code);

            if(!metaLoginResponse.getIsNewUser()){
                CookieUtil.addUserToken(response, metaLoginResponse.getUserToken());
            }

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(metaLoginResponse.getHtmlResponse());
        } catch (Exception e) {
            log.error("Meta 로그인 에러", e);

            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("Meta 로그인 실패: " + e.getMessage());
        }
    }




}
