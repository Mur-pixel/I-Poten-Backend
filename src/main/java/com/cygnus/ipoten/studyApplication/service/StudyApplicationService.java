package com.cygnus.ipoten.studyApplication.service;

import com.cygnus.ipoten.studyApplication.service.request.CreateStudyApplicationRequest;
import com.cygnus.ipoten.studyApplication.service.request.ProcessApplicationRequest;
import com.cygnus.ipoten.studyApplication.service.response.ApplicationForHostResponse;
import com.cygnus.ipoten.studyApplication.service.response.CreateStudyApplicationResponse;
import com.cygnus.ipoten.studyApplication.service.response.ListMyApplicationResponse;
import com.cygnus.ipoten.studyApplication.service.response.MyApplicationStatusResponse;

import java.util.List;

public interface StudyApplicationService {
    CreateStudyApplicationResponse applyToStudy(CreateStudyApplicationRequest request);

    List<ListMyApplicationResponse> findMyApplications(Long applicantId);

    void cancelApplication(Long applicationId, Long applicantId);

    MyApplicationStatusResponse findMyApplicationStatus(Long studyRoomId, Long applicantId);

    List<ApplicationForHostResponse> findApplicationsForHost(Long studyRoomId, Long hostId);

    void processApplication(Long applicationId, Long hostId, ProcessApplicationRequest request);
}
