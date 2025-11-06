package com.cygnus.ipoten.user_term.service.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class RenameUserWordbookFolderResponse {
    Long id;
    String folderName;
    Integer sortOrder;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
