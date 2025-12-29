package com.cygnus.ipoten.wordbook.controller.response_form;

import com.cygnus.ipoten.wordbook.service.response.MoveFavoritesResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MoveFavoritesResponseForm {
    private Long targetWordbookId;
    private int movedCount;
    private int skippedCount;
    private List<Item> skipped; // 중복/권한 등 스킵 사유

    @Getter @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private Long termId;
        private String reason; // DUPLICATE_IN_TARGET | NOT_FOUND_FAVORITE | FORBIDDEN | UNKNOWN
    }

    public static MoveFavoritesResponseForm from(MoveFavoritesResponse response) {
        var list = response.getSkipped().stream()
                .map(s -> new Item(s.getTermId(), s.getReason().name()))
                .toList();
        return new MoveFavoritesResponseForm(response.getTargetWordbookId(), response.getMovedCount(), list.size(), list);
    }
}
