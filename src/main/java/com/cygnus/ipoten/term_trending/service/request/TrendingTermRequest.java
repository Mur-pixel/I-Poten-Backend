package com.cygnus.ipoten.term_trending.service.request;

public record TrendingTermRequest(
        String range,
        int limit
) {
}
