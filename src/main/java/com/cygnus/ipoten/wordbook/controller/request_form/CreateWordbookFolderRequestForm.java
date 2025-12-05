package com.cygnus.ipoten.wordbook.controller.request_form;

import com.cygnus.ipoten.wordbook.service.request.CreateWordbookFolderRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateWordbookFolderRequestForm {

    @NotBlank
    private String folderName;

    public CreateWordbookFolderRequest toCreateFolderRequest(Long accountId) {
        return new CreateWordbookFolderRequest(accountId, folderName);
    }
}
