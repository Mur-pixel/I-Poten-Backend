package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.user_term.service.response.MoveFavoritesResponse;

import java.util.List;

public interface UserWordbookTermService {
    void addToStarFolder(Long accountId, Long termId);
    void removeFromStarFolder(Long accountId, Long termId);
    MoveFavoritesResponse moveFromStarFolder(Long accountId, Long targetFolderId, List<Long> termIds);
}
