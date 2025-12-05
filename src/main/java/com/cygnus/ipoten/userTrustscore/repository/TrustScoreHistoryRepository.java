package com.cygnus.ipoten.userTrustscore.repository;

import com.cygnus.ipoten.userTrustscore.entity.TrustScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TrustScoreHistoryRepository extends JpaRepository<TrustScoreHistory, Long> {

    // 특정 유저의 히스토리를 최근순으로 조회
    List<TrustScoreHistory> findByAccountIdOrderByRecordedAtDesc(Long accountId);

    // 특정 accountId + 기간(recordedAt)으로 존재 여부 확인
    boolean existsByAccountIdAndRecordedAtBetween(
            Long accountId,
            LocalDateTime start,
            LocalDateTime end
    );

    void deleteAllByAccountId(Long accountId);
}
