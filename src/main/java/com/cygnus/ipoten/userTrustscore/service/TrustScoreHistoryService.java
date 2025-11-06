package com.cygnus.ipoten.userTrustscore.service;

import com.cygnus.ipoten.userTrustscore.controller.response.TrustScoreHistoryResponse;

import java.util.List;

public interface TrustScoreHistoryService {

    void recordMonthlyScore(Long accountId, double score);
    List<TrustScoreHistoryResponse> getHistory(Long accountId);
}
