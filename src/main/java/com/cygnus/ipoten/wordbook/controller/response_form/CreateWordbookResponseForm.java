package com.cygnus.ipoten.wordbook.controller.response_form;

import com.cygnus.ipoten.wordbook.service.response.CreateWordbookResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateWordbookResponseForm {
    private final Long id;
    private final String wordbookName;
    private final Integer sortOrder;

    public static CreateWordbookResponseForm from(CreateWordbookResponse response) {
        return new CreateWordbookResponseForm(
                response.getId(),
                response.getWordbookName(),
                response.getSortOrder()
        );
    }
}
