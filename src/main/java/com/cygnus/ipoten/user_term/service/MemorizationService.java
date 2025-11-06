package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.user_term.service.request.UpdateMemorizationRequest;
import com.cygnus.ipoten.user_term.service.response.UpdateMemorizationResponse;

public interface MemorizationService {
    UpdateMemorizationResponse updateMemorization(UpdateMemorizationRequest request);
}
