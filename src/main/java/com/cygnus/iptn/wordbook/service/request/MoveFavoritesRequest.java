package com.cygnus.iptn.wordbook.service.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MoveFavoritesRequest {
    private final Long accountId;
    private final Long targetWordbookId;
    private final List<Long> termIds;
    private final List<Long> favoriteIds;
}
