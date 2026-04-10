package com.cygnus.iptn.interview.service;

import com.cygnus.iptn.interview.controller.request.InterviewEndRequest;
import com.cygnus.iptn.interview.controller.request_form.*;
import com.cygnus.iptn.interview.controller.response_form.NormalInterviewCreateResponseForm;
import com.cygnus.iptn.interview.controller.response_form.PersonalityInterviewResultResponseForm;
import com.cygnus.iptn.interview.entity.Interview;
import com.cygnus.iptn.interview.entity.InterviewType;
import com.cygnus.iptn.interview.service.response.*;

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
    void submitPersonalityInterviewAnswers(NormalInterviewSubmitRequestForm form, Long accountId);
    PersonalityInterviewResultResponseForm getPersonalityInterviewResult(Long interviewId, Long accountId);





}
