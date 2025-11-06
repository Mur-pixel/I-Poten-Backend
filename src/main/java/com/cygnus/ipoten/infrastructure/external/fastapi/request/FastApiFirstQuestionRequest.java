package com.cygnus.ipoten.infrastructure.external.fastapi.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FastApiFirstQuestionRequest {

    private Long interviewId;             // 인터뷰 ID
    private Integer topic;               // 인터뷰 주제 (정수로 변경)
    private Integer experienceLevel;     // 경험 수준 (정수로 변경)
    private Integer academicBackground;  // 학문적 배경 (정수로 변경)
    private String companyName;          // 회사 이름
    private Long questionId;             // 질문 ID
    private String answerText;           // 답변 텍스트
    private String userToken;            // 사용자 토큰


    public FastApiFirstQuestionRequest(Long interviewId, Integer topic, Integer experienceLevel, Integer academicBackground, String companyName, Long questionId, String answerText, String userToken) {
        this.interviewId = interviewId;
        this.topic = topic;
        this.experienceLevel = experienceLevel;
        this.academicBackground = academicBackground;
        this.companyName = companyName;
        this.questionId = questionId;
        this.answerText = answerText;
        this.userToken = userToken;
    }
}
