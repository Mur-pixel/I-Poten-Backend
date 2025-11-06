package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.user_term.entity.UserTermProgress;
import com.cygnus.ipoten.user_term.repository.UserTermProgressRepository;
import com.cygnus.ipoten.user_term.service.request.UpdateMemorizationRequest;
import com.cygnus.ipoten.user_term.service.response.UpdateMemorizationResponse;
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
public class MemorizationServiceImpl implements MemorizationService {

    private final UserTermProgressRepository userTermProgressRepository;
    private final TermRepository termRepository;
    private final EntityManager em;
    private final UserWordbookFolderQueryService userWordbookFolderQueryService;

    @Transactional
    @Override
    public UpdateMemorizationResponse updateMemorization(UpdateMemorizationRequest request) {

        final Long accountId = request.getAccountId();
        final Long termId = request.getTermId();

        termRepository.findById(request.getTermId())
                .orElseThrow(() -> new IllegalArgumentException("용어가 존재하지 않습니다."));

        var accRef  = em.getReference(com.cygnus.ipoten.account.entity.Account.class, request.getAccountId());
        var termRef = em.getReference(com.cygnus.ipoten.term.entity.Term.class, request.getTermId());

        var id = new UserTermProgress.Id(request.getAccountId(), request.getTermId());

        var existing = userTermProgressRepository.findById(id);
        final boolean isNew = existing.isEmpty();

        var progress = existing.orElseGet(() -> UserTermProgress.newOf(accRef, termRef));

        var before = progress.getStatus();
        boolean changed = (before != request.getStatus());

        log.info("[svc] progress before change: status={}, memorizedAt={}",
                progress.getStatus(), progress.getMemorizedAt());

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
                userWordbookFolderQueryService.evictMyFoldersStatsCache(accountId);
            }
        });

        return UpdateMemorizationResponse.builder()
                .termId(request.getTermId())
                .status(progress.getStatus())
                .memorizedAt(progress.getMemorizedAt())
                .lastStudiedAt(progress.getLastStudiedAt())
                .changed(changed)
                .build();
    }
}
