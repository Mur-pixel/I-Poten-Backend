package com.cygnus.iptn.wordbook.service.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CreateWordbookTermResponse {
    private Long userWordbookTermId;
    private Long folderId;
    private Long termId;
    private boolean created;
    private boolean alreadyAttached;

    public static CreateWordbookTermResponse created(Long uwtId, Long folderId, Long termId) {
        return CreateWordbookTermResponse.builder()
                .userWordbookTermId(uwtId)
                .folderId(folderId)
                .termId(termId)
                .created(true)
                .alreadyAttached(false)
                .build();
    }

    public static CreateWordbookTermResponse alreadyAttached(Long uwtId, Long folderId, Long termId) {
        return CreateWordbookTermResponse.builder()
                .userWordbookTermId(uwtId)
                .folderId(folderId)
                .termId(termId)
                .created(false)
                .alreadyAttached(true)
                .build();
    }
}
