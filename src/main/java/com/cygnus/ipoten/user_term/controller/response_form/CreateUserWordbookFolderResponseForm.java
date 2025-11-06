package com.cygnus.ipoten.user_term.controller.response_form;

import com.cygnus.ipoten.user_term.service.response.CreateUserWordbookFolderResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateUserWordbookFolderResponseForm {
    private final Long id;
    private final String folderName;
    private final Integer sortOrder;

    public static CreateUserWordbookFolderResponseForm from(CreateUserWordbookFolderResponse response) {
        return new CreateUserWordbookFolderResponseForm(
                response.getId(),
                response.getFolderName(),
                response.getSortOrder()
        );
    }
}
