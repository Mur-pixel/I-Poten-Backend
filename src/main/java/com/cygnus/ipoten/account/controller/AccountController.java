package com.cygnus.ipoten.account.controller;

import com.cygnus.ipoten.account.controller.request_form.RegisterRequestForm;
import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.account.service.SignupService;
import com.cygnus.ipoten.account.service.register_response.RegisterResponse;
import com.cygnus.ipoten.common.annotation.LoginToken;
import com.cygnus.ipoten.common.annotation.PublicEndpoint;
import com.cygnus.ipoten.common.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {

    private final SignupService signupService;
    private final AccountService accountService;

    // 네이버, 구글 통합 약관 동의 후 회원 가입 — temporaryToken 헤더 사용
    @PublicEndpoint
    @PostMapping("/signup")
    public ResponseEntity<RegisterResponse> signup(
            @RequestHeader("Authentication") String temporaryUserToken,
            @RequestBody RegisterRequestForm registerRequestForm,
            HttpServletResponse response) {

        log.info("Signup request - 회원가입 호출 완료");

        RegisterResponse signupResult = signupService.signup(temporaryUserToken, registerRequestForm);

        CookieUtil.addUserToken(response, signupResult.getUserToken());

        return ResponseEntity.ok(signupResult);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@LoginToken String userToken) {
        log.info("회원탈퇴 접근");
        try {
            accountService.withdraw(userToken);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("회원 탈퇴 요청에서 오류 발생 : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
