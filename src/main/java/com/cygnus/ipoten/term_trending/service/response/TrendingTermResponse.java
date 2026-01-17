package com.cygnus.ipoten.term_trending.service.response;

import java.time.LocalDateTime;
import java.util.List;

public record TrendingTermResponse(
        String range,
        int limit,
        List<Item> items
) {
    public record Item(
            Long termId,
            String title,
            long searchCount,
            LocalDateTime lastSearchedAt
    ) {}
}
