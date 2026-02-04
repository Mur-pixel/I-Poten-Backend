package com.cygnus.ipoten.interview.controller.response_form;


import com.cygnus.ipoten.interview.entity.CandidateStatus;
import com.cygnus.ipoten.interview.entity.InterviewType;
import com.cygnus.ipoten.interview.service.response.InterviewWithAudio;
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
