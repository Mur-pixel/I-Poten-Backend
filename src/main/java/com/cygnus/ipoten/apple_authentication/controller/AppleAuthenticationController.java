package com.cygnus.ipoten.apple_authentication.controller;

import com.cygnus.ipoten.apple_authentication.controller.request.AppleLoginMobileRequest;
import com.cygnus.ipoten.apple_authentication.service.AppleAuthenticationService;
import com.cygnus.ipoten.apple_authentication.service.mobile_response.AppleLoginMobileResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AppleAuthenticationController {

    private final AppleAuthenticationService appleAuthenticationService;

    @Value("${apple.android-package-name}")
    private String androidPackageName;

    @RequestMapping(value = "/spring/apple-authentication/login", method = {RequestMethod.GET, RequestMethod.POST})
    public void appleLoginCallback(
            @RequestParam MultiValueMap<String, String> params,
            HttpServletResponse response
    ) throws IOException {
        String queryString = params.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(value -> encode(entry.getKey()) + "=" + encode(value)))
                .collect(Collectors.joining("&"));

        String redirectUrl = "intent://callback"
                + (queryString.isEmpty() ? "" : "?" + queryString)
                + "#Intent;package=" + androidPackageName + ";scheme=signinwithapple;end";

        log.info("Apple callback -> Android redirect");
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/authentication/apple/login/mobile")
    public ResponseEntity<AppleLoginMobileResponse> appleLoginMobile(
            @RequestBody AppleLoginMobileRequest request
    ) {
        try {
            AppleLoginMobileResponse appleLoginMobileResponse = appleAuthenticationService.handleLoginMobile(request);
            return new ResponseEntity<>(appleLoginMobileResponse, HttpStatus.OK);
        } catch (Exception e) {
            log.error("애플 모바일 로그인 오류", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
