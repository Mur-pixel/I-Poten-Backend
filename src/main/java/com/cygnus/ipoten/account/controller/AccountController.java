package com.cygnus.ipoten.account.controller;

import com.cygnus.ipoten.account.controller.request_form.RegisterRequestForm;
import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.account.service.SignupService;
import com.cygnus.ipoten.account.service.register_response.RegisterResponse;
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


    //네이버 약관 동의 후 회원 가입
    @PostMapping("/signup")
    public ResponseEntity<RegisterResponse> signup(
            @RequestHeader("Authentication") String temporaryUserToken,
            @RequestBody RegisterRequestForm registerRequestForm,
            HttpServletResponse response) {   // <- response 추가

        log.info("Signup request - 회원가입 호출 완료");

        // 회원가입 서비스 호출
        RegisterResponse signupResult = signupService.signup(temporaryUserToken, registerRequestForm);

        // 회원가입 성공 후 새 userToken 쿠키 설정
        String cookieHeader = String.format(
                "userToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict",
                signupResult.getUserToken(),  // service에서 발급한 토큰 사용
                6 * 60 * 60                   // 6시간
        );
        response.addHeader("Set-Cookie", cookieHeader);

        return ResponseEntity.ok(signupResult);
    }




    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@CookieValue(name="userToken", required = false) String userToken){
        log.info("회원탈퇴 접근");

        try {
            accountService.withdraw(userToken);
            return ResponseEntity.ok().build();
        }catch (Exception e) {
            log.info("회원 탈퇴 요청에서 오류 발생 : {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }



}
