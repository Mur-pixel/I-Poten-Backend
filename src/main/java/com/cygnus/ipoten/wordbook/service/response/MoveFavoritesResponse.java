package com.cygnus.ipoten.wordbook.service.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class MoveFavoritesResponse {
    private final Long targetWordbookId;
    private final int movedCount;
    private final List<Skipped> skipped;

    @Getter @AllArgsConstructor
    public static class Skipped {
        public enum Reason { DUPLICATE_IN_TARGET, NOT_FOUND, FAVORITE, FORBIDDEN, UNKNOWN }
        private final Long termId;
        private final Reason reason;
    }

    public static MoveFavoritesResponse empty(Long targetWordbookId) {
        return new MoveFavoritesResponse(targetWordbookId, 0, new ArrayList<>());
    }
}
