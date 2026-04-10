package com.cygnus.iptn.term_log.service;

import com.cygnus.iptn.term.entity.Term;
import com.cygnus.iptn.term.service.request.SearchTermRequest;
import org.springframework.data.domain.Page;

public interface TermSearchLogService {
    /** 사용자가 특정 용어를 "검색/조회"했다는 이벤트를 기록
     * - 내부적으로 term_search_stats_daily에 카운트 적재
     */
    void recordTermSearched(Long termId, String actorKey);
    void recordTrendingLogIfMappable(String q, Page<Term> page, SearchTermRequest request);

    void recordSearchRequestLog(
            String actorKey, String queryRaw, String queryNorm, int resultCount, boolean isZero, int latencyMs, Long selectedCategoryId, String sortKey, boolean includeTags
    );
}
