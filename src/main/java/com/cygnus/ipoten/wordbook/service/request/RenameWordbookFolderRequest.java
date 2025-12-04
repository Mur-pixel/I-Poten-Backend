package com.cygnus.ipoten.wordbook.service.request;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RenameWordbookFolderRequest {
    Long accountId;
    Long folderId;
    String folderName;
}
