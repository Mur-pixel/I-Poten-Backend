package com.cygnus.ipoten.user_term.service.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class MoveFavoritesResponse {
    private final Long targetFolderId;
    private final int movedCount;
    private final List<Skipped> skipped;

    @Getter @AllArgsConstructor
    public static class Skipped {
        public enum Reason { DUPLICATE_IN_TARGET, NOT_FOUND, FAVORITE, FORBIDDEN, UNKNOWN}
        private final Long termId;
        private final Reason reason;
    }

    public static MoveFavoritesResponse empty(Long targetFolderId) {
        return new MoveFavoritesResponse(targetFolderId, 0, new ArrayList<>());
    }
}
