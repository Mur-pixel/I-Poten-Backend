package com.cygnus.iptn.interview.service.response;

import com.cygnus.iptn.interview.controller.response_form.InterviewProgressResponseForm;
import com.cygnus.iptn.interview.controller.response_form.NormalInterviewCreateResponseForm;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Builder
public class NormalInterviewProgressResponse {

    private Long interviewId;
    private List<InterviewWithAudio> interviewList;

    public NormalInterviewProgressResponse(List<InterviewWithAudio> interviewList) {
        this.interviewList = interviewList;
    }

    public NormalInterviewProgressResponse(Long interviewId) {
        this.interviewId = interviewId;
    }

    public NormalInterviewProgressResponse(Long interviewId, List<InterviewWithAudio> interviewList) {
        this.interviewId = interviewId;
        this.interviewList = interviewList;
    }

    public NormalInterviewCreateResponseForm toNormalInterviewCreateResponseForm() {
        return new NormalInterviewCreateResponseForm(this.interviewId, this.interviewList);
    }


}
