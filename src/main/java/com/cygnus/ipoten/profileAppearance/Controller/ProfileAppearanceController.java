package com.cygnus.ipoten.profileAppearance.Controller;

import com.cygnus.ipoten.profileAppearance.Controller.response.AccountSummaryResponse;
import com.cygnus.ipoten.profileAppearance.Controller.response.AppearanceResponse;
import com.cygnus.ipoten.profileAppearance.Service.*;
import com.cygnus.ipoten.userDashboard.service.TokenAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile-appearance")
@RequiredArgsConstructor
public class ProfileAppearanceController {

    private final ProfileAppearanceService appearanceService;
    private final TokenAccountService tokenAccountService;
    private final AccountSummaryService accountSummaryService;

    /**
     * 내 프로필 외형 정보 조회
     */
    @GetMapping("/my")
    public ResponseEntity<AppearanceResponse> getMyAppearance(
            @CookieValue(name = "userToken", required = false) String userToken
    ){
        Long accountId = tokenAccountService.resolveAccountId(userToken);

        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401
        }

        AppearanceResponse response = appearanceService.getMyAppearance(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * 기본 회원 요약 정보 조회 (로그인타입 + 연속 출석)
     */
    @GetMapping("/basic")
    public ResponseEntity<AccountSummaryResponse> getMyBasicSummary(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AccountSummaryResponse response = accountSummaryService.getBasicSummary(accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * 프로필 사진 업로드 (업로드 Presigned URL 발급)
     */
    @PostMapping("/photo/upload-url")
    public ResponseEntity<String> getUploadUrl(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestParam("filename") String filename,
            @RequestParam("contentType") String contentType
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        return ResponseEntity.ok(
                appearanceService.generateUploadUrl(accountId, filename, contentType)
        );
    }

    /**
     * 프로필 사진 다운로드 (다운로드 Presigned URL 발급)
     */
    @GetMapping("/photo/download-url")
    public ResponseEntity<String> getDownloadUrl(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        return ResponseEntity.ok(
                appearanceService.generateDownloadUrl(accountId)
        );
    }
}