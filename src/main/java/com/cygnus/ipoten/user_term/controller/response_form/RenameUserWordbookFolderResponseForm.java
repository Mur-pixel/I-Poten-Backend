package com.cygnus.ipoten.user_term.controller.response_form;

import com.cygnus.ipoten.user_term.service.response.RenameUserWordbookFolderResponse;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class RenameUserWordbookFolderResponseForm {
    Long id;
    String folderName;
    Integer sortOrder;
    LocalDateTime updatedAt;

    public static RenameUserWordbookFolderResponseForm from(RenameUserWordbookFolderResponse r) {
        return RenameUserWordbookFolderResponseForm.builder()
                .id(r.getId())
                .folderName(r.getFolderName())
                .sortOrder(r.getSortOrder())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
