package com.cygnus.iptn.term.service.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchTermResponse {
    @Getter @Builder
    public static class TermSummary {
        private final Long id;
        private final String title;
        private final String description;
    }
    private final long total;
    private final java.util.List<TermSummary> items;
}
