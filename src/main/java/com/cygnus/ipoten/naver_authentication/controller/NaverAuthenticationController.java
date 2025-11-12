package com.cygnus.ipoten.naver_authentication.controller;


import com.cygnus.ipoten.naver_authentication.service.NaverAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    public String login(@RequestParam String code){
        log.info("login code: {}", code);
        return "테스트 성공";
    }
}
