package com.cygnus.ipoten.interview.controller;

import com.cygnus.ipoten.interview.controller.response_form.UserTechStackResponse;
import com.cygnus.ipoten.interview.service.UserTechStackService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview")
public class UserTechStackController {

    private final RedisCacheService redisCacheService;
    private final UserTechStackService userTechStackService;

    @GetMapping("/my-techstack")
    public ResponseEntity<UserTechStackResponse> getUserTechStack(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        return ResponseEntity.ok(userTechStackService.getUserTechStack(accountId));
    }
}
