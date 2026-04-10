package com.cygnus.iptn.interview.service.request;

import com.cygnus.iptn.interview.entity.InterviewType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InterviewSequenceRequest {

    private Long interviewId;
    private Long interviewQAId;
    private InterviewType interviewType;
    private int interviewSequence;
    private String answer;

    public InterviewSequenceRequest(Long interviewId, Long interviewQAId, int interviewSequence, String answer) {
        this.interviewId = interviewId;
        this.interviewQAId = interviewQAId;
        this.interviewSequence = interviewSequence;
        this.answer = answer;
    }
}
