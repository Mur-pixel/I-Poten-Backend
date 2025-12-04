package com.cygnus.ipoten.wordbook.controller.request_form;

import com.cygnus.ipoten.wordbook.service.request.ReorderWordbookFoldersRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderWordbookFoldersRequestForm {

    @NotEmpty(message = "ids는 비어 있을 수 없습니다.")
    private List<@NotNull(message = "id에는 null이 올 수 없습니다.") Long> ids;

    public ReorderWordbookFoldersRequest toRequest(Long accountId) {
        return ReorderWordbookFoldersRequest.builder()
                .accountId(accountId)
                .orderedIds(ids)
                .build();
    }
}
