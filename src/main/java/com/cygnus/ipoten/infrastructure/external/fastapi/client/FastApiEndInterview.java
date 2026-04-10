package com.cygnus.ipoten.infrastructure.external.fastapi.client;

import com.cygnus.ipoten.interview.controller.request.InterviewEndRequest;

public interface FastApiEndInterview {

     void endInterview(InterviewEndRequest request);

}
