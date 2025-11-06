package com.cygnus.ipoten.interview.service;

import com.cygnus.ipoten.interview.controller.request.InterviewEndRequest;
import com.cygnus.ipoten.interview.controller.request_form.InterviewCreateRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.InterviewEndRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.InterviewProgressRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.entity.InterviewType;
import com.cygnus.ipoten.interview.service.response.InterviewCreateResponse;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interview.service.response.InterviewResultListResponse;
import com.cygnus.ipoten.interview.service.response.InterviewResultResponse;

import java.util.List;
import java.util.Optional;

public interface InterviewService {

    InterviewCreateResponse createInterview(InterviewCreateRequestForm interviewCreateRequestForm, Long accountId, String userToken);
    InterviewProgressResponse execute(InterviewType type, InterviewProgressRequestForm form, String userToken);
    void endInterview(InterviewEndRequestForm interviewEndRequestForm, String userToken);
    InterviewEndRequest createEndInterviewRequestEndInterviewRequest(InterviewEndRequestForm interviewEndRequestForm, String userToken);
    Optional<Interview> findById(Long id);
    InterviewResultResponse interviewResult(InterviewResultRequestForm interviewResultRequestForm);
    List<InterviewResultListResponse> getInterviewResultListByAccountId(Long accountId);





}
