package com.cygnus.iptn.account_project.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account_project.entity.AccountProject;
import com.cygnus.iptn.interview.controller.request.InterviewAccountProjectRequest;

import java.util.List;

public interface AccountProjectService {

    List<AccountProject> findAllByAccount_IdAndIsActiveTrue(Long accountId);
    void saveAllByInterviewAccountProjectRequest(List<InterviewAccountProjectRequest> interviewAccountProjectRequests, Account account);

}
