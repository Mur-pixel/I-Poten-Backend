package com.cygnus.ipoten.user_term.service;

import com.cygnus.ipoten.user_term.service.request.RecordTermViewRequest;
import com.cygnus.ipoten.user_term.service.response.RecordTermViewResponse;

public interface UserRecentTermService {
    RecordTermViewResponse recordTermView(RecordTermViewRequest request);
}
