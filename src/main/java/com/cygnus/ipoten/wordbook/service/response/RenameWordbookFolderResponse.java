package com.cygnus.ipoten.wordbook.service.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class RenameWordbookFolderResponse {
    Long id;
    String folderName;
    Integer sortOrder;
    Instant createdAt;
    Instant updatedAt;
}
