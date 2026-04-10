package com.cygnus.iptn.interview.controller.response_form;


import com.cygnus.iptn.interview.entity.CandidateStatus;
import com.cygnus.iptn.interview.entity.InterviewType;
import com.cygnus.iptn.interview.service.response.InterviewWithAudio;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NormalInterviewCreateResponseForm {

    private Long interviewId;
    private List<InterviewWithAudio> interviewList;

    public NormalInterviewCreateResponseForm(Long interviewId, List<InterviewWithAudio> interviewList) {
        this.interviewId = interviewId;
        this.interviewList = interviewList;
    }
}
