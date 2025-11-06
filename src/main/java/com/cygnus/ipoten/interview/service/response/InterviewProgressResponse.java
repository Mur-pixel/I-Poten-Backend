package com.cygnus.ipoten.interview.service.response;

import com.cygnus.ipoten.interview.controller.response_form.InterviewProgressResponseForm;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class InterviewProgressResponse {

    private Long interviewQAId;
    private Long interviewId;
    private String interviewQuestion;

    public InterviewProgressResponse(Long interviewQAId, Long interviewId, String interviewQuestion) {
        this.interviewQAId = interviewQAId;
        this.interviewId = interviewId;
        this.interviewQuestion = interviewQuestion;
    }


    public InterviewProgressResponseForm toInterviewProgressResponseForm(){
        return new InterviewProgressResponseForm(this.interviewQAId, this.interviewId, this.interviewQuestion);
    }

    public InterviewCreateResponse toInterviewCreateResponse(){
        return new InterviewCreateResponse(
                this.interviewQuestion,
                this.interviewId,
                this.interviewQAId
        );
    }

}
