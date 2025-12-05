package com.cygnus.ipoten.studyApplication.service.response;

import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MyApplicationStatusResponse {
    private final Long applicationId;
    private final ApplicationStatus status;
}