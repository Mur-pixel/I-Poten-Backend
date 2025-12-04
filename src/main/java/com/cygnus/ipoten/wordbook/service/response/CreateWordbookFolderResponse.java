package com.cygnus.ipoten.wordbook.service.response;

import com.cygnus.ipoten.wordbook.entity.WordbookFolder;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
public class CreateWordbookFolderResponse {
    private final Long id;
    private final String folderName;
    private final Integer sortOrder;
    private final String message;
    private final String createdAt;

    public static CreateWordbookFolderResponse from(WordbookFolder f) {
        return CreateWordbookFolderResponse.builder()
                .id(f.getId())
                .folderName(f.getFolderName())
                .sortOrder(f.getSortOrder())
                .message("폴더가 생성되었습니다.")
                .createdAt(f.getCreatedAt().toString())
                .build();
    }
}
