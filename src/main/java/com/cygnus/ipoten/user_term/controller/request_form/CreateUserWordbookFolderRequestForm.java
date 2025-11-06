package com.cygnus.ipoten.user_term.controller.request_form;

import com.cygnus.ipoten.user_term.service.request.CreateUserWordbookFolderRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateUserWordbookFolderRequestForm {

    @NotBlank
    private String folderName;

    public CreateUserWordbookFolderRequest toCreateFolderRequest(Long accountId) {
        return new CreateUserWordbookFolderRequest(accountId, folderName);
    }
}
