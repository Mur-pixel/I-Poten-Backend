package com.cygnus.ipoten.studyApplication.service.request;

import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProcessApplicationRequest {
    private ApplicationStatus status;
}
