package com.cygnus.ipoten.userTitle.controller;

import com.cygnus.ipoten.userDashboard.service.TokenAccountService;
import com.cygnus.ipoten.userTitle.controller.response.UserTitleResponse;
import com.cygnus.ipoten.userTitle.service.UserTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-titles")
@RequiredArgsConstructor
public class UserTitleController {

    private final TokenAccountService tokenAccountService;
    private final UserTitleService titleService;

    /**
     * 내가 보유한 칭호 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<UserTitleResponse>> getMyTitles(
            @CookieValue(name = "userToken", required = false) String userToken
    ){
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        return ResponseEntity.ok(titleService.getMyTitles(accountId));
    }

    /**
     * 특정 칭호 장착
     */
    @PutMapping("/{titleId}/equip")
    public ResponseEntity<UserTitleResponse> equipTitle(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long titleId
    ){
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        return ResponseEntity.ok(titleService.equipTitle(accountId, titleId));
    }

    /**
     * 특정 칭호 장착 해제
     */
    @PutMapping("/unequip")
    public ResponseEntity<Void> unequipTitle(
            @CookieValue(name = "userToken", required = false) String userToken
    ){
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        titleService.unequipTitle(accountId);
        return ResponseEntity.ok().build();
    }
}
