package com.cygnus.ipoten.infrastructure.external.fastapi.client;

import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiFirstQuestionRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.response.FastApiQuestionResponse;

public interface FastApiFirstFollowupQuestionClient {

    FastApiQuestionResponse getFastApiFirstFollowupQuestion(FastApiFirstQuestionRequest request);

}
