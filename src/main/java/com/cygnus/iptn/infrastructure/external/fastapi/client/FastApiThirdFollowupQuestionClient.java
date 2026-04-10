package com.cygnus.iptn.infrastructure.external.fastapi.client;

import com.cygnus.iptn.infrastructure.external.fastapi.request.FastApiThirdProgressRequest;
import com.cygnus.iptn.infrastructure.external.fastapi.response.FastApiQuestionResponse;


public interface FastApiThirdFollowupQuestionClient {

    FastApiQuestionResponse getFastApiThirdFollowupQuestion(FastApiThirdProgressRequest request);


}
