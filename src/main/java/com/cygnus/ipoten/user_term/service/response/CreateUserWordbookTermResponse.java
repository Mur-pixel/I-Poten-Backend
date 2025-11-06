package com.cygnus.ipoten.user_term.service.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateUserWordbookTermResponse {
    private Long userWordbookTermId;
    private Long folderId;
    private Long termId;
    private boolean created;
    private boolean alreadyAttached;

    public static CreateUserWordbookTermResponse created(Long uwtId, Long folderId, Long termId) {
        return CreateUserWordbookTermResponse.builder()
                .userWordbookTermId(uwtId)
                .folderId(folderId)
                .termId(termId)
                .created(true)
                .alreadyAttached(false)
                .build();
    }

    public static CreateUserWordbookTermResponse alreadyAttached(Long uwtId, Long folderId, Long termId) {
        return CreateUserWordbookTermResponse.builder()
                .userWordbookTermId(uwtId)
                .folderId(folderId)
                .termId(termId)
                .created(false)
                .alreadyAttached(true)
                .build();
    }
}
