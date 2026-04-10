package com.cygnus.iptn.wordbook.service.request;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RenameWordbookRequest {
    Long accountId;
    Long wordbookId;
    String wordbookName;
}
