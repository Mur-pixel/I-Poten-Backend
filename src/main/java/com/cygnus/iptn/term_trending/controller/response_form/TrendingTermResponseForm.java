package com.cygnus.iptn.term_trending.controller.response_form;

import com.cygnus.iptn.term_trending.service.response.TrendingTermResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class TrendingTermResponseForm {

    private final String range;
    private final int limit;
    private final List<Item> items;

    private TrendingTermResponseForm(String range, int limit, List<Item> items) {
        this.range = range;
        this.limit = limit;
        this.items = items;
    }

    public static TrendingTermResponseForm from(TrendingTermResponse response) {
        List<Item> items = response.items().stream()
                .map(Item::from)
                .toList();
        return new TrendingTermResponseForm(response.range(), response.limit(), items);
    }

    @Getter
    public static class Item {
        private final Long termId;
        private final String title;
        private final long searchCount;
        private final LocalDateTime lastSearchedAt;

        private Item(Long termId, String title, long searchCount, LocalDateTime lastSearchedAt) {
            this.termId = termId;
            this.title = title;
            this.searchCount = searchCount;
            this.lastSearchedAt = lastSearchedAt;
        }

        public static Item from(TrendingTermResponse.Item item) {
            return new Item(item.termId(), item.title(), item.searchCount(), item.lastSearchedAt());
        }
    }
}
