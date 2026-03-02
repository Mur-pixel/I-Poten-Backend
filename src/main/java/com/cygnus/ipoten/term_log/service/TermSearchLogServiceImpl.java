package com.cygnus.ipoten.term_log.service;

import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.service.request.SearchTermRequest;
import com.cygnus.ipoten.term_log.entity.TermSearchLog;
import com.cygnus.ipoten.term_log.repository.TermSearchLogRepository;
import com.cygnus.ipoten.term_trending.repository.TermSearchStatsDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermSearchLogServiceImpl implements TermSearchLogService {

    private final TermSearchStatsDailyRepository termSearchStatsDailyRepository;
    private final TermSearchLogRepository termSearchLogRepository;

    @Override
    @Transactional
    public void recordTermSearched(Long termId, String actorKey) {
        if (termId == null) return;
        String ak = (actorKey == null || actorKey.isBlank()) ? "anon" : actorKey;
        log.info("[term-search] termId={}, actorKey={}", termId, ak);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // Redis 없이: 검색/조회 이벤트가 올 때마다 오늘자 카운트 +1
        termSearchStatsDailyRepository.upsert(today, now, termId);
    }

    @Override
    @Transactional
    public void recordTrendingLogIfMappable(String q, Page<Term> page, SearchTermRequest request) {
        if (q == null || q.isBlank()) return;

        if (request.isPrefixMode()) return;

        List<Term> items = page.getContent();
        if (items == null || items.isEmpty()) return;

        // 규칙 1) 결과가 1개면 그 Term을 "검색한 것"으로 본다
        if (items.size() == 1) {
            recordTermSearched(items.get(0).getId(), request.getActorKey());
            return;
        }

        // 규칙 2) title이 q와 정확히 일치하는 결과가 있으면 그 Term 기록
        String qq = q.trim().toLowerCase();
        for (Term t : items) {
            if (t.getTitle() != null && t.getTitle().trim().toLowerCase().equals(qq)) {
                recordTermSearched(t.getId(), request.getActorKey());
                return;
            }
        }

        // 규칙 3) 매칭 실패하면 1등 결과를 기록(트렌딩 데이터 확보 목적)
        recordTermSearched(items.get(0).getId(), request.getActorKey());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSearchRequestLog(String actorKey, String queryRaw, String queryNorm, int resultCount, boolean isZero, int latencyMs, Long selectedCategoryId, String sortKey, boolean includeTags
    ) {
        if (actorKey == null || actorKey.isBlank()) return;

        TermSearchLog termSearchLog = TermSearchLog.create(
                actorKey, queryRaw, queryNorm, resultCount, isZero, latencyMs, selectedCategoryId, sortKey, includeTags
        );

        termSearchLogRepository.save(termSearchLog);
    }
}