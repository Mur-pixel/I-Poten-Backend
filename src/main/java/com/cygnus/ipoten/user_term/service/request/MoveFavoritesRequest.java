package com.cygnus.ipoten.user_term.service.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MoveFavoritesRequest {
    private final Long accountId;
    private final Long targetFolderId;
    private final List<Long> termIds;
    private final List<Long> favoriteIds;
}
