package com.cygnus.ipoten.user_term.controller.request_form;

import com.cygnus.ipoten.user_term.service.request.MoveFavoritesRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MoveFavoritesRequestForm {
    @NotNull
    private Long targetFolderId;
    private List<Long> termIds;
    private List<Long> favoriteIds;

    public MoveFavoritesRequest toRequest(Long accountId) {
        return new MoveFavoritesRequest(accountId, targetFolderId, termIds, favoriteIds);
    }
}
