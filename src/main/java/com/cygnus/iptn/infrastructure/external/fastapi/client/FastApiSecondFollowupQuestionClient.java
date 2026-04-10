package com.cygnus.iptn.infrastructure.external.fastapi.client;

import com.cygnus.iptn.infrastructure.external.fastapi.request.FastApiSecondProgressRequest;
import com.cygnus.iptn.infrastructure.external.fastapi.response.FastApiQuestionResponse;


public interface FastApiSecondFollowupQuestionClient {

    FastApiQuestionResponse getFastApiSecondFollowupQuestion(FastApiSecondProgressRequest request);

}
