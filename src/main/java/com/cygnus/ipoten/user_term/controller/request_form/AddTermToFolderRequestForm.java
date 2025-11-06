package com.cygnus.ipoten.user_term.controller.request_form;

import com.cygnus.ipoten.user_term.service.request.CreateUserWordbookTermRequest;
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

    public CreateUserWordbookTermRequest toRequest(Long accountId, Long folderId) {
        return CreateUserWordbookTermRequest.builder()
                .accountId(accountId)
                .folderId(folderId)
                .termId(termId)
                .build();
    }
}
