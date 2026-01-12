package com.cygnus.ipoten.wordbook.service.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class RenameWordbookResponse {
    Long id;
    String wordbookName;
    Integer sortOrder;
    Instant createdAt;
    Instant updatedAt;
}
