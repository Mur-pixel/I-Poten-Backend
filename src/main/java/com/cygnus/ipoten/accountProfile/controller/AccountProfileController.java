package com.cygnus.ipoten.accountProfile.controller;

import com.cygnus.ipoten.accountProfile.controller.request.NicknameRequest;
import com.cygnus.ipoten.accountProfile.controller.response.EmailResponse;
import com.cygnus.ipoten.accountProfile.controller.response.NicknameResponse;
import com.cygnus.ipoten.accountProfile.controller.response.ProfileResponse;
import com.cygnus.ipoten.accountProfile.controller.response.UpdateNicknameResponse;
import com.cygnus.ipoten.accountProfile.service.AccountProfileService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account-profile")
@RequiredArgsConstructor
public class AccountProfileController {

    private final AccountProfileService accountProfileService;
    private final RedisCacheService redisCacheService;

    @PutMapping("/update-nickname")
    public ResponseEntity<UpdateNicknameResponse> updateNickname(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody NicknameRequest request) {

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        UpdateNicknameResponse response = accountProfileService.updateNickname(accountId, request.getNickname())
                .orElseThrow(() -> new IllegalArgumentException("닉네임 변경 실패"));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/nickname")
    public ResponseEntity<NicknameResponse>  getNickname(
            @CookieValue(name = "userToken", required = false) String userToken) {
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        NicknameResponse nicknameResponse = accountProfileService.getNicknameByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("회원의 닉네임을 찾을 수 없습니다"));

        return ResponseEntity.ok(nicknameResponse);
    }

    @GetMapping("/email")
    public ResponseEntity<EmailResponse> getEmail(
            @CookieValue(name = "userToken", required = false) String userToken) {
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        EmailResponse emailResponse = accountProfileService.getEmailByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("회원의 이메일을 찾을 수 없습니다"));

        return ResponseEntity.ok(emailResponse);
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(
            @CookieValue(name = "userToken", required = false) String userToken) {
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        ProfileResponse profileResponse = accountProfileService.getProfileByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("회원의 회원정보를 찾을 수 없습니다"));

        return ResponseEntity.ok(profileResponse);
    }


}