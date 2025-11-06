package com.cygnus.ipoten.infrastructure.external.fastapi.client;

import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiFourthProgressRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.response.FastApiQuestionResponse;

public interface FastApiFourthFollowupQuestionClient {

    FastApiQuestionResponse getFastApiFourthFollowupQuestion(FastApiFourthProgressRequest request);

}
