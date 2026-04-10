package com.cygnus.ipoten.meta_authentication.controller;

import com.cygnus.ipoten.authentication.social.SocialLoginException;
import com.cygnus.ipoten.authentication.social.SocialLoginPopupResponseBuilder;
import com.cygnus.ipoten.common.util.CookieUtil;
import com.cygnus.ipoten.config.FrontendConfig;
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
    private final FrontendConfig frontendConfig;
    private final SocialLoginPopupResponseBuilder popupResponseBuilder;

    @GetMapping("/link")
    public String metaOauthLink() {
        return metaAuthenticationService.requestKakaoOauthLink();
    }

    @GetMapping("/login")
    public void login(@RequestParam("code") String code, HttpServletResponse response) throws Exception {
        try {
            MetaLoginResponse metaLoginResponse = metaAuthenticationService.handleLogin(code);

            if (!metaLoginResponse.getIsNewUser()) {
                CookieUtil.addUserToken(response, metaLoginResponse.getUserToken());
            }

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(metaLoginResponse.getHtmlResponse());
        } catch (SocialLoginException e) {
            response.setStatus(e.getStatus().value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(
                    popupResponseBuilder.buildErrorHtml(e.toResponse(), frontendConfig.getOrigins().get(0))
            );
        } catch (Exception e) {
            log.error("meta login error", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("Meta 로그인 실패: " + e.getMessage());
        }
    }
}
