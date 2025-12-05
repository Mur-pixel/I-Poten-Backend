package com.cygnus.ipoten.wordbook.controller.request_form;

import com.cygnus.ipoten.wordbook.service.request.CreateWordbookTermRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddTermToFolderRequestForm {
    @NotNull
    private Long termId;

    public CreateWordbookTermRequest toRequest(Long accountId, Long folderId) {
        return CreateWordbookTermRequest.builder()
                .accountId(accountId)
                .folderId(folderId)
                .termId(termId)
                .build();
    }
}
