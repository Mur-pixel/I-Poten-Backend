package com.cygnus.ipoten.interview_result.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "interview_result_detail")
public class InterviewResultDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long interviewResultId;

    @Lob
    private String question;    // 질문
    @Lob
    private String answer;      // 답변
    @Lob
    private String intent;      // 의도 파악
    @Lob
    private String feedback;    // 피드백
    @Lob
    private String correction;  // 첨삭


    public InterviewResultDetail(Long interviewResultId, String question, String answer, String intent, String feedback) {
        this.interviewResultId = interviewResultId;
        this.question = question;
        this.answer = answer;
        this.intent = intent;
        this.feedback = feedback;
        this.correction = correction;
    }

    public InterviewResultDetail() {

    }
}
