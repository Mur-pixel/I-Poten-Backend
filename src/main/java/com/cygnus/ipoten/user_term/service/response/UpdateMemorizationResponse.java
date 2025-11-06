package com.cygnus.ipoten.user_term.service.response;

import com.cygnus.ipoten.user_term.entity.enums.MemorizationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UpdateMemorizationResponse {
    private final Long termId;
    private final MemorizationStatus status;
    private final LocalDateTime memorizedAt;
    private final LocalDateTime lastStudiedAt;
    private final boolean changed;
}
