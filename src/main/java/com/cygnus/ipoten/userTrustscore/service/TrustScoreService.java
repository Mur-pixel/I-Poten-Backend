package com.cygnus.ipoten.userTrustscore.service;

import com.cygnus.ipoten.userTrustscore.controller.response.TrustScoreResponse;

public interface TrustScoreService {

    void initTrustScore(Long accountId);

    // 단순 조회
    TrustScoreResponse getTrustScore(Long accountId);

    // 계산 + 저장
    TrustScoreResponse calculateTrustScore(Long accountId);
}
