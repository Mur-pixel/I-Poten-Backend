package com.cygnus.ipoten.user_term.controller.request_form;

import com.cygnus.ipoten.user_term.service.request.ReorderUserWordbookFoldersRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderUserWordbookFoldersRequestForm {

    @NotEmpty(message = "ids는 비어 있을 수 없습니다.")
    private List<@NotNull(message = "id에는 null이 올 수 없습니다.") Long> ids;

    public ReorderUserWordbookFoldersRequest toRequest(Long accountId) {
        return ReorderUserWordbookFoldersRequest.builder()
                .accountId(accountId)
                .orderedIds(ids)
                .build();
    }
}
