package com.cygnus.ipoten.term_trending.service;

import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.service.request.SearchTermRequest;
import org.springframework.data.domain.Page;

public interface TermSearchEventService {
    /** 사용자가 특정 용어를 "검색/조회"했다는 이벤트를 기록
     * - 내부적으로 term_search_stats_daily에 카운트 적재
     */
    void recordTermSearched(Long termId, String actorKey);
    void recordTrendingEventIfMappable(String q, Page<Term> page, SearchTermRequest request);
}
