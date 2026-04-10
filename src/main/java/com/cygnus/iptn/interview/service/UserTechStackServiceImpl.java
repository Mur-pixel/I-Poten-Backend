package com.cygnus.iptn.interview.service;

import com.cygnus.iptn.interview.controller.response_form.UserTechStackResponse;
import com.cygnus.iptn.interview.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserTechStackServiceImpl implements UserTechStackService {

    private final InterviewRepository interviewRepository;

    @Override
    public UserTechStackResponse getUserTechStack(Long accountId) {
        return interviewRepository.findTopByAccountIdAndIsFinishedTrueOrderByCreatedAtDesc(accountId)
                .map(interview -> {
                    var profile = interview.getIntervieweeProfile();
                    var stacks = profile.getTechStack().stream()
                            .map(stack -> new UserTechStackResponse.TechStackDto(stack.name(), stack.getDisplayName()))
                            .toList();

                    return UserTechStackResponse.builder()
                            .hasInterview(true)
                            .job(profile.getJob())
                            .techStacks(stacks)
                            .createdAt(interview.getCreatedAt().toString())
                            .build();
                })
                .orElse(UserTechStackResponse.noInterview());
    }
}
