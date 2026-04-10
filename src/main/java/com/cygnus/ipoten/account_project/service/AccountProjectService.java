package com.cygnus.ipoten.account_project.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account_project.entity.AccountProject;
import com.cygnus.ipoten.interview.controller.request.InterviewAccountProjectRequest;

import java.util.List;

public interface AccountProjectService {

    List<AccountProject> findAllByAccount_IdAndIsActiveTrue(Long accountId);
    void saveAllByInterviewAccountProjectRequest(List<InterviewAccountProjectRequest> interviewAccountProjectRequests, Account account);

}
