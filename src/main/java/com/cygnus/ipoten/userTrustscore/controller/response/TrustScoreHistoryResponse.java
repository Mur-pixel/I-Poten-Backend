package com.cygnus.ipoten.userTrustscore.controller.response;

import com.cygnus.ipoten.userTrustscore.entity.TrustScoreHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrustScoreHistoryResponse {
    private double totalScore;
    private LocalDateTime recordedAt;

    public static TrustScoreHistoryResponse fromEntity(TrustScoreHistory entity) {
        return TrustScoreHistoryResponse.builder()
                .totalScore(entity.getTotalScore())
                .recordedAt(entity.getRecordedAt())
                .build();
    }
}
