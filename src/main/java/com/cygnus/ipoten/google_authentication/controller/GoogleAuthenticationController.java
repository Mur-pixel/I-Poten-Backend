package com.cygnus.ipoten.google_authentication.controller;

import com.cygnus.ipoten.google_authentication.service.GoogleAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
