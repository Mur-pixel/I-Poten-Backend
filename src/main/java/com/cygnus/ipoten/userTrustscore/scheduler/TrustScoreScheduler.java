package com.cygnus.ipoten.userTrustscore.scheduler;

import com.cygnus.ipoten.userTrustscore.entity.TrustScore;
import com.cygnus.ipoten.userTrustscore.repository.TrustScoreRepository;
import com.cygnus.ipoten.userTrustscore.service.TrustScoreHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrustScoreScheduler {

    private final TrustScoreRepository trustScoreRepository;
    private final TrustScoreHistoryService historyService;

    /**
     * 매달 1일 새벽 0시 5분에 전체 유저 대상으로 실행
     * account_id | score | recorded_at
     * -----------+-------+---------------------
     * 1          | 72.5  | 2025-09-30 23:59:59
     * 2          | 61.0  | 2025-09-30 23:59:59
     */
    @Scheduled(cron = "0 5 0 1 * ?")
    public void archiveMonthlyScores() {
        List<TrustScore> all = trustScoreRepository.findAll();

        log.info("🕓 TrustScoreScheduler started: {} accounts found", all.size());

        for (TrustScore ts : all) {
            try {
                historyService.recordMonthlyScore(ts.getAccountId(), ts.getTotalScore());
            } catch (Exception e) {
                log.error("❌ Failed to record trust score history for accountId={}", ts.getAccountId(), e);
            }
        }

        log.info("✅ TrustScoreScheduler finished successfully");
    }
}
