package com.cygnus.iptn.interview.service.response;

import com.cygnus.iptn.interview.controller.response_form.InterviewProgressResponseForm;
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
    private String interviewQuestionText;

    public InterviewProgressResponse(Long interviewQAId, Long interviewId, String interviewQuestion, String interviewQuestionText) {
        this.interviewQAId = interviewQAId;
        this.interviewId = interviewId;
        this.interviewQuestion = interviewQuestion;
        this.interviewQuestionText = interviewQuestionText;
    }

    public InterviewProgressResponse(Long interviewQAId, Long interviewId,String interviewQuestionText) {
        this.interviewQAId = interviewQAId;
        this.interviewId = interviewId;
        this.interviewQuestionText = interviewQuestionText;
    }


    public InterviewProgressResponseForm toInterviewProgressResponseForm(){
        return new InterviewProgressResponseForm(this.interviewQAId, this.interviewId, this.interviewQuestion, this.interviewQuestionText);
    }

    public InterviewCreateResponse toInterviewCreateResponseByTTS(String tts, String text) {
        return new InterviewCreateResponse(
                tts,
                this.interviewId,
                this.interviewQAId,
                text
        );
    }

    public InterviewCreateResponse toInterviewCreateResponse(){
        return new InterviewCreateResponse(
                this.interviewQuestion,
                this.interviewId,
                this.interviewQAId,
                this.interviewQuestionText
        );
    }

    public InterviewProgressResponse updateInterviewQuestion(String tts, String text) {
        return new InterviewProgressResponse(
                this.interviewQAId,
                this.interviewId,
                tts,
                text
        );
    }

}
