package com.cygnus.iptn.wordbook.controller.request_form;

import com.cygnus.iptn.wordbook.service.request.CreateWordbookTermRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddTermToWordbookRequestForm {
    @NotNull
    private Long termId;

    public CreateWordbookTermRequest toRequest(Long accountId, Long wordbookId) {
        return CreateWordbookTermRequest.builder()
                .accountId(accountId)
                .wordbookId(wordbookId)
                .termId(termId)
                .build();
    }
}
