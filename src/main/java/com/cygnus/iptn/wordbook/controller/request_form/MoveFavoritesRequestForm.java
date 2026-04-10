package com.cygnus.iptn.wordbook.controller.request_form;

import com.cygnus.iptn.wordbook.service.request.MoveFavoritesRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MoveFavoritesRequestForm {
    @NotNull
    private Long targetWordbookId;
    private List<Long> termIds;
    private List<Long> favoriteIds;

    public MoveFavoritesRequest toRequest(Long accountId) {
        return new MoveFavoritesRequest(accountId, targetWordbookId, termIds, favoriteIds);
    }
}
