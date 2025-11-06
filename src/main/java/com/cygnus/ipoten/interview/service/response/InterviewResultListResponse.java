package com.cygnus.ipoten.interview.service.response;

import com.cygnus.ipoten.interview.entity.InterviewType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class InterviewResultListResponse {

    private boolean isFinished;

    private LocalDateTime createdAt;

    private String sender;

    private InterviewType interviewType;

    private Long interviewId;

    public InterviewResultListResponse(boolean isFinished, LocalDateTime createdAt, String sender, InterviewType interviewType, Long interviewId) {
        this.isFinished = isFinished;
        this.createdAt = createdAt;
        this.sender = sender;
        this.interviewType = interviewType;
        this.interviewId = interviewId;
    }



}
