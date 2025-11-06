package com.cygnus.ipoten.infrastructure.external.fastapi.request;

import com.cygnus.ipoten.interviewee_profile.entity.TechStack;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FastApiThirdProgressRequest {

    private String userToken;
    private Long interviewId;
    private int projectExperience;
    private Long questionId;
    private String answerText;
    private Integer topic;               // 인터뷰 주제 (정수로 변경)
    private Integer experienceLevel;     // 경험 수준 (정수로 변경)
    private Integer academicBackground;  // 학문적 배경 (정수로 변경)
    private List<FastApiAccountProjectRequest> projectResponses;
    private List<TechStack> techStack;
    private String companyName;


}
