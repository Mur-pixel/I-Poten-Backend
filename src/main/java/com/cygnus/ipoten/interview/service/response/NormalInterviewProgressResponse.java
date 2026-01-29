package com.cygnus.ipoten.interview.service.response;

import com.cygnus.ipoten.interview.controller.response_form.InterviewProgressResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.NormalInterviewCreateResponseForm;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Builder
public class NormalInterviewProgressResponse {

    private Long interviewId;
    private List<String> interviewList;

    public NormalInterviewProgressResponse(List<String> interviewList) {
        this.interviewList = interviewList;
    }

    public NormalInterviewProgressResponse(Long interviewId) {
        this.interviewId = interviewId;
    }

    public NormalInterviewProgressResponse(Long interviewId, List<String> interviewList) {
        this.interviewId = interviewId;
        this.interviewList = interviewList;
    }

    public NormalInterviewCreateResponseForm toNormalInterviewCreateResponseForm() {
        return new NormalInterviewCreateResponseForm(this.interviewId, this.interviewList);
    }


}
