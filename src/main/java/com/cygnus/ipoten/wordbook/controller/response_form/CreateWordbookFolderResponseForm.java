package com.cygnus.ipoten.wordbook.controller.response_form;

import com.cygnus.ipoten.wordbook.service.response.CreateWordbookFolderResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateWordbookFolderResponseForm {
    private final Long id;
    private final String folderName;
    private final Integer sortOrder;

    public static CreateWordbookFolderResponseForm from(CreateWordbookFolderResponse response) {
        return new CreateWordbookFolderResponseForm(
                response.getId(),
                response.getFolderName(),
                response.getSortOrder()
        );
    }
}
