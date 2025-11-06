package com.cygnus.ipoten.account_project.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account_project.entity.AccountProject;
import com.cygnus.ipoten.account_project.repository.AccountProjectRepository;
import com.cygnus.ipoten.interview.controller.request.InterviewAccountProjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountProjectServiceImpl implements AccountProjectService {

    private final AccountProjectRepository accountProjectRepository;

    @Transactional
    @Override
    public List<AccountProject> findAllByAccount_IdAndIsActiveTrue(Long accountId) {
        return accountProjectRepository.findAllByAccount_IdAndIsActiveTrue(accountId);
    }

    @Override
    public void saveAllByInterviewAccountProjectRequest(List<InterviewAccountProjectRequest> interviewAccountProjectRequests, Account account) {

        List<AccountProject> accountProjects = interviewAccountProjectRequests.stream()
                .map(dto -> new AccountProject(
                        account,
                        dto.getProjectName(),
                        dto.getProjectDescription()
                ))
                .toList();

        accountProjectRepository.saveAll(accountProjects);
    }

}
