package com.cygnus.ipoten.user_term.controller.response_form;

import com.cygnus.ipoten.user_term.service.response.CreateUserWordbookTermResponse;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateUserWordbookTermResponseForm {
    private Long userWordbookTermId;
    private Long folderId;
    private Long termId;
    private boolean created;
    private boolean alreadyAttached;

    public static CreateUserWordbookTermResponseForm from(CreateUserWordbookTermResponse r) {
        return CreateUserWordbookTermResponseForm.builder()
                .userWordbookTermId(r.getUserWordbookTermId())
                .folderId(r.getFolderId())
                .termId(r.getTermId())
                .created(r.isCreated())
                .alreadyAttached(r.isAlreadyAttached())
                .build();
    }
}
