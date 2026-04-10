package com.cygnus.iptn.infrastructure.external.fastapi.client;

import com.cygnus.iptn.infrastructure.external.fastapi.request.FastApiFirstQuestionRequest;
import com.cygnus.iptn.infrastructure.external.fastapi.response.FastApiQuestionResponse;

public interface FastApiFirstFollowupQuestionClient {

    FastApiQuestionResponse getFastApiFirstFollowupQuestion(FastApiFirstQuestionRequest request);

}
