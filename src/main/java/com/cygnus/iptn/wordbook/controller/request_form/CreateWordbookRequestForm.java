package com.cygnus.iptn.wordbook.controller.request_form;

import com.cygnus.iptn.wordbook.service.request.CreateWordbookRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateWordbookRequestForm {

    @NotBlank
    private String wordbookName;

    public CreateWordbookRequest toCreateFolderRequest(Long accountId) {
        return new CreateWordbookRequest(accountId, wordbookName);
    }
}
