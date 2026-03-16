package com.cygnus.ipoten.interview.service;

import com.cygnus.ipoten.interview.controller.request.InterviewEndRequest;
import com.cygnus.ipoten.interview.controller.request_form.*;
import com.cygnus.ipoten.interview.controller.response_form.NormalInterviewCreateResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.PersonalityInterviewResultResponseForm;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.entity.InterviewType;
import com.cygnus.ipoten.interview.service.response.*;

import java.util.List;
import java.util.Optional;

public interface InterviewService {

    InterviewCreateResponse createInterview(InterviewCreateRequestForm interviewCreateRequestForm, Long accountId, String userToken);

    NormalInterviewProgressResponse createNormalInterview(List<InterviewWithAudio> interviewList,NormalInterviewCreateRequestForm normalInterviewCreateRequestForm, Long accountId);
    InterviewProgressResponse execute(InterviewType type, InterviewProgressRequestForm form, String userToken);
    NormalInterviewCreateResponseForm execute(InterviewType type, NormalInterviewCreateRequestForm form, String userToken);
    void endInterview(InterviewEndRequestForm interviewEndRequestForm, String userToken);
    InterviewEndRequest createEndInterviewRequestEndInterviewRequest(InterviewEndRequestForm interviewEndRequestForm, String userToken);
    Optional<Interview> findById(Long id);
    InterviewResultResponse interviewResult(InterviewResultRequestForm interviewResultRequestForm);
    List<InterviewResultListResponse> getInterviewResultListByAccountId(Long accountId);
    void submitPersonalityInterviewAnswers(NormalInterviewSubmitRequestForm form);
    PersonalityInterviewResultResponseForm getPersonalityInterviewResult(Long interviewId);





}
