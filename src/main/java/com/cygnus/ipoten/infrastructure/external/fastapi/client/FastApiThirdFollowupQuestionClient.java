package com.cygnus.ipoten.infrastructure.external.fastapi.client;

import com.cygnus.ipoten.infrastructure.external.fastapi.request.FastApiThirdProgressRequest;
import com.cygnus.ipoten.infrastructure.external.fastapi.response.FastApiQuestionResponse;


public interface FastApiThirdFollowupQuestionClient {

    FastApiQuestionResponse getFastApiThirdFollowupQuestion(FastApiThirdProgressRequest request);


}
