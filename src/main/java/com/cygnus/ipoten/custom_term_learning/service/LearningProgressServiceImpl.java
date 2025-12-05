package com.cygnus.ipoten.custom_term_learning.service;

import com.cygnus.ipoten.wordbook.service.WordbookFolderQueryService;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.custom_term_learning.entity.LearningProgress;
import com.cygnus.ipoten.custom_term_learning.repository.LearningProgressRepository;
import com.cygnus.ipoten.custom_term_learning.service.request.UpdateLearningProgressRequest;
import com.cygnus.ipoten.custom_term_learning.service.response.UpdateLearningProgressResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningProgressServiceImpl implements LearningProgressService {

    private final LearningProgressRepository learningProgressRepository;
    private final TermRepository termRepository;
    private final EntityManager em;
    private final WordbookFolderQueryService wordbookFolderQueryService;

    @Transactional
    @Override
    public UpdateLearningProgressResponse updateMemorization(UpdateLearningProgressRequest request) {

        final Long accountId = request.getAccountId();
        final Long termId = request.getTermId();

        termRepository.findById(request.getTermId())
                .orElseThrow(() -> new IllegalArgumentException("용어가 존재하지 않습니다."));

        var accRef  = em.getReference(com.cygnus.ipoten.account.entity.Account.class, request.getAccountId());
        var termRef = em.getReference(com.cygnus.ipoten.term.entity.Term.class, request.getTermId());

        var id = new LearningProgress.Id(request.getAccountId(), request.getTermId());

        var existing = learningProgressRepository.findById(id);
        final boolean isNew = existing.isEmpty();

        var progress = existing.orElseGet(() -> LearningProgress.newOf(accRef, termRef));

        var before = progress.getStatus();
        boolean changed = (before != request.getStatus());

        log.info("[svc] progress before change: status={}, completedAt={}",
                progress.getStatus(), progress.getCompletedAt());

        if (changed) {
            progress.changeStatus(request.getStatus());
        }

        // 매 요청마다 최근 학습일 갱신 (상태가 동일해도 기록)
        progress.markStudiedNow();

        // 새 객체면 persist(기존은 변경감지)
        if (isNew) {
            em.persist(progress);
        }

        // 폴더 통계 시 캐시 무효화 (lastStudiedAt 집계 반영)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                wordbookFolderQueryService.evictMyFoldersStatsCache(accountId);
            }
        });

        return UpdateLearningProgressResponse.builder()
                .termId(request.getTermId())
                .status(progress.getStatus())
                .completedAt(progress.getCompletedAt())
                .lastStudiedAt(progress.getLastStudiedAt())
                .changed(changed)
                .build();
    }
}
