package com.cygnus.ipoten.wordbook.service.response;

import com.cygnus.ipoten.wordbook.entity.Wordbook;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class CreateWordbookResponse {
    private final Long id;
    private final String wordbookName;
    private final Integer sortOrder;
    private final String message;
    private final String createdAt;

    public static CreateWordbookResponse from(Wordbook w) {
        return CreateWordbookResponse.builder()
                .id(w.getId())
                .wordbookName(w.getWordbookName())
                .sortOrder(w.getSortOrder())
                .message("폴더가 생성되었습니다.")
                .createdAt(w.getCreatedAt().toString())
                .build();
    }
}
