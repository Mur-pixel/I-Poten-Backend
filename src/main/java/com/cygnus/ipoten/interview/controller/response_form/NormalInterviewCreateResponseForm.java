package com.cygnus.ipoten.interview.controller.response_form;


import com.cygnus.ipoten.interview.entity.CandidateStatus;
import com.cygnus.ipoten.interview.entity.InterviewType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NormalInterviewCreateResponseForm {

    private Long interviewId;
    private List<String> interviewList;

    public NormalInterviewCreateResponseForm(Long interviewId, List<String> interviewList) {
        this.interviewId = interviewId;
        this.interviewList = interviewList;
    }
}
