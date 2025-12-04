package com.cygnus.ipoten.wordbook.service;

import com.cygnus.ipoten.wordbook.service.response.MoveFavoritesResponse;

import java.util.List;

public interface WordbookTermService {
    void removeFromStarFolder(Long accountId, Long termId);
    MoveFavoritesResponse moveFromStarFolder(Long accountId, Long targetFolderId, List<Long> termIds);
}
