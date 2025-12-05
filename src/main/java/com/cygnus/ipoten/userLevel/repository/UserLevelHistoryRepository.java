package com.cygnus.ipoten.userLevel.repository;

import com.cygnus.ipoten.userLevel.entity.UserLevelHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLevelHistoryRepository extends JpaRepository<UserLevelHistory, Long> {

    // 특정 유저의 레벨업 이력(최신순)
    List<UserLevelHistory> findByAccountIdOrderByAchievedAtDesc(Long accountId);

    // 페이징 지원(프론트 무한 스크롤/페이지네이션용)
    Page<UserLevelHistory> findByAccountId(Long accountId, Pageable pageable);

    void deleteAllByAccountId(Long accountId);
}
