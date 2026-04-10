package com.cygnus.iptn.infrastructure.external.fastapi.client;

import com.cygnus.iptn.infrastructure.external.fastapi.request.FastApiFourthProgressRequest;
import com.cygnus.iptn.infrastructure.external.fastapi.response.FastApiQuestionResponse;

public interface FastApiFourthFollowupQuestionClient {

    FastApiQuestionResponse getFastApiFourthFollowupQuestion(FastApiFourthProgressRequest request);

}
