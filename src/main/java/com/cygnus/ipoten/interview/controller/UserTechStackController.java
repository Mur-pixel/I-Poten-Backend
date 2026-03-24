package com.cygnus.ipoten.interview.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.interview.controller.response_form.UserTechStackResponse;
import com.cygnus.ipoten.interview.service.UserTechStackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview")
public class UserTechStackController {

    private final UserTechStackService userTechStackService;

    @GetMapping("/my-techstack")
    public ResponseEntity<UserTechStackResponse> getUserTechStack(@LoginUser Long accountId) {
        return ResponseEntity.ok(userTechStackService.getUserTechStack(accountId));
    }
}
