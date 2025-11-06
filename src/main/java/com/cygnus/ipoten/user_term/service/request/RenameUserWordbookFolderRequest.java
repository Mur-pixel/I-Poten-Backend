package com.cygnus.ipoten.user_term.service.request;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RenameUserWordbookFolderRequest {
    Long accountId;
    Long folderId;
    String folderName;
}
