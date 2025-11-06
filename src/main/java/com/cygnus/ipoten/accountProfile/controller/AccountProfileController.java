package com.cygnus.ipoten.accountProfile.controller;

import com.cygnus.ipoten.accountProfile.controller.request.NicknameRequest;
import com.cygnus.ipoten.accountProfile.controller.response.NicknameResponse;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.userDashboard.service.TokenAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account-profile")
@RequiredArgsConstructor
public class AccountProfileController {
    private final AccountProfileService accountProfileService;
    private final TokenAccountService tokenAccountService;

    @PutMapping("/update-nickname")
    public ResponseEntity<NicknameResponse> updateNickname(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody NicknameRequest request) {

        Long accountId = tokenAccountService.resolveAccountId(userToken);

        NicknameResponse response = accountProfileService.updateNickname(accountId, request.getNickname())
                .orElseThrow(() -> new IllegalArgumentException("닉네임 변경 실패"));

        return ResponseEntity.ok(response);
    }
}