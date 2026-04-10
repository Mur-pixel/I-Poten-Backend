package com.cygnus.iptn.term_trending.service.request;

public record TrendingTermRequest(
        String range,
        int limit
) {
}
