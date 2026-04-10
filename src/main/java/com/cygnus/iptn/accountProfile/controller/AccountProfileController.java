package com.cygnus.iptn.accountProfile.controller;

import com.cygnus.iptn.accountProfile.controller.request.NicknameRequest;
import com.cygnus.iptn.accountProfile.controller.response.EmailResponse;
import com.cygnus.iptn.accountProfile.controller.response.NicknameResponse;
import com.cygnus.iptn.accountProfile.controller.response.ProfileResponse;
import com.cygnus.iptn.accountProfile.controller.response.UpdateNicknameResponse;
import com.cygnus.iptn.accountProfile.service.AccountProfileService;
import com.cygnus.iptn.common.annotation.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping({"/api/me", "/account-profile"})
@RequiredArgsConstructor
public class AccountProfileController {

    private final AccountProfileService accountProfileService;

    @PutMapping("/update-nickname")
    public ResponseEntity<UpdateNicknameResponse> updateNickname(
            @LoginUser Long accountId,
            @RequestBody NicknameRequest request) {

        UpdateNicknameResponse response = accountProfileService.updateNickname(accountId, request.getNickname())
                .orElseThrow(() -> new IllegalArgumentException("닉네임 변경 실패"));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/nickname")
    public ResponseEntity<NicknameResponse> getNickname(@LoginUser Long accountId) {

        NicknameResponse nicknameResponse = accountProfileService.getNicknameByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("회원의 닉네임을 찾을 수 없습니다"));
        return ResponseEntity.ok(nicknameResponse);
    }

    @GetMapping("/email")
    public ResponseEntity<EmailResponse> getEmail(@LoginUser Long accountId) {
        EmailResponse emailResponse = accountProfileService.getEmailByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("회원의 이메일을 찾을 수 없습니다"));
        return ResponseEntity.ok(emailResponse);
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(@LoginUser Long accountId) {
        ProfileResponse profileResponse = accountProfileService.getProfileByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("회원의 회원정보를 찾을 수 없습니다"));
        return ResponseEntity.ok(profileResponse);
    }

    @GetMapping({"", "/"})
    public ResponseEntity<ProfileResponse> getMe(
            @LoginUser Long accountId) {
        return getProfile(accountId);
    }

    private Long resolveAccountId(@LoginUser Long accountId, String userToken) {

        if (accountId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return accountId;
    }
}
