package com.cygnus.ipoten.wordbook.controller.response_form;

import com.cygnus.ipoten.wordbook.service.response.RenameWordbookFolderResponse;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class RenameWordbookFolderResponseForm {
    Long id;
    String folderName;
    Integer sortOrder;
    Instant updatedAt;

    public static RenameWordbookFolderResponseForm from(RenameWordbookFolderResponse r) {
        return RenameWordbookFolderResponseForm.builder()
                .id(r.getId())
                .folderName(r.getFolderName())
                .sortOrder(r.getSortOrder())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
