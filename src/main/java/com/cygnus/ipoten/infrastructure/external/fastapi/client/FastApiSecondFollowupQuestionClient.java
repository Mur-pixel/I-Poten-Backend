package com.cygnus.ipoten.infrastructure.external.fastapi.client;

import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiSecondProgressRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.response.FastApiQuestionResponse;


public interface FastApiSecondFollowupQuestionClient {

    FastApiQuestionResponse getFastApiSecondFollowupQuestion(FastApiSecondProgressRequest request);

}
