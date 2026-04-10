package com.cygnus.iptn.github_authentication.controller;

import com.cygnus.iptn.account.entity.LoginType;
import com.cygnus.iptn.account.service.AccountService;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.github_authentication.service.GithubAuthenticationService;
import com.cygnus.iptn.github_authentication.service.response.GithubLoginResponse;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/github-authentication")
@RequiredArgsConstructor
public class GithubAuthenticationController {
    final private GithubAuthenticationService githubAuthenticationService;
    final private AccountService accountService;
    final private AccountProfileService accountProfileService;
    final private RedisCacheService redisCacheService;
    final private LoginType GithubType =LoginType.GITHUB;

    @GetMapping("/request-login-url")
    public String requestGetLoginLink() {
        //log.info("requestGetLoginLink() called");
        String loginLink = githubAuthenticationService.getLoginLink();
        //log.info("requestGetLoginLink() returned {}", loginLink);
        if(loginLink == null){
            log.info("loginLink is null");
            return null;
        }
        return loginLink;
    }

    @GetMapping("/login")
    @Transactional
    public void GithubLogin(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        log.info("requestAccessToken(): code {}", code);
        try {
            GithubLoginResponse githubLoginResponse = githubAuthenticationService.handleLogin(code);
            String cookieHeader = String.format(
                    "userToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
                    githubLoginResponse.getUserToken(),
                  1 * 60 * 60 // 1시간
            );        // CSRF 방어
            response.addHeader("Set-Cookie", cookieHeader);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(githubLoginResponse.getHtmlResponse());
        } catch (Exception e) {
            log.error("Github 로그인 에러", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("Github 로그인 실패: " + e.getMessage());
        }
    }
}