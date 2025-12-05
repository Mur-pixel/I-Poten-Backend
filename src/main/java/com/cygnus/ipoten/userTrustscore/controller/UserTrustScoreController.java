package com.cygnus.ipoten.userTrustscore.controller;

import com.cygnus.ipoten.userDashboard.service.TokenAccountService;
import com.cygnus.ipoten.userTrustscore.controller.response.TrustScoreHistoryResponse;
import com.cygnus.ipoten.userTrustscore.controller.response.TrustScoreResponse;
import com.cygnus.ipoten.userTrustscore.service.TrustScoreHistoryService;
import com.cygnus.ipoten.userTrustscore.service.TrustScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trust-score")
@RequiredArgsConstructor
public class UserTrustScoreController {

    private final TokenAccountService tokenAccountService;
    private final TrustScoreService trustScoreService;
    private final TrustScoreHistoryService trustScoreHistoryService;

    /**
     * 최신 신뢰점수 조회
     */
    @GetMapping
    public ResponseEntity<TrustScoreResponse> getTrustScore(
            @CookieValue(name = "userToken", required = false) String userToken
    ){
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        return ResponseEntity.ok(trustScoreService.getTrustScore(accountId));
    }

    /**
     * 최신 신뢰점수 갱신 (실시간 계산 후 trust_score 테이블 업데이트)
     * - trust_score_history는 월말 스케줄러에서 별도 기록
     */
    @PostMapping("/calculate")
    public ResponseEntity<TrustScoreResponse> calculateTrustScore(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        return ResponseEntity.ok(trustScoreService.calculateTrustScore(accountId));
    }

    /**
     * 월별 신뢰점수 히스토리 조회
     */
    @GetMapping("/history")
    public ResponseEntity<List<TrustScoreHistoryResponse>> getTrustScoreHistory(
            @CookieValue(name = "userToken", required = false) String userToken
    ){
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        return ResponseEntity.ok(trustScoreHistoryService.getHistory(accountId));
    }
}
