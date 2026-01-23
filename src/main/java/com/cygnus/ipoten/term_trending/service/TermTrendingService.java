package com.cygnus.ipoten.term_trending.service;

import com.cygnus.ipoten.term_trending.service.request.TrendingTermRequest;
import com.cygnus.ipoten.term_trending.service.response.TrendingTermResponse;

public interface TermTrendingService {
    TrendingTermResponse getTrending(TrendingTermRequest request);
}
