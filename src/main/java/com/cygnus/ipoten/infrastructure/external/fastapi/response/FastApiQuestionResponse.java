package com.cygnus.ipoten.infrastructure.external.fastapi.response;

import lombok.Getter;

import java.util.List;

@Getter
public class FastApiQuestionResponse {

    private Long interviewId;
    private List<String> questions;
    private List<Long> questionIds;

}
