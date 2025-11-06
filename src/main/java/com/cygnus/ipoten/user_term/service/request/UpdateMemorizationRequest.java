package com.cygnus.ipoten.user_term.service.request;

import com.cygnus.ipoten.user_term.entity.enums.MemorizationStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateMemorizationRequest {
    private final Long accountId;
    private final Long termId;
    private final MemorizationStatus status;
}
