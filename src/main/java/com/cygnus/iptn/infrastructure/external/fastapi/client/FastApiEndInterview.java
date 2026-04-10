package com.cygnus.iptn.infrastructure.external.fastapi.client;

import com.cygnus.iptn.interview.controller.request.InterviewEndRequest;

public interface FastApiEndInterview {

     void endInterview(InterviewEndRequest request);

}
