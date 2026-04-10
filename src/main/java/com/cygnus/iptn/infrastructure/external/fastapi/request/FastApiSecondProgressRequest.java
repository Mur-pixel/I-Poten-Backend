package com.cygnus.iptn.infrastructure.external.fastapi.request;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FastApiSecondProgressRequest {

    private String userToken;
    private Long interviewId;
    private int projectExperience;
    private Long questionId;
    private List<FastApiAccountProjectRequest> projectResponses;


}
