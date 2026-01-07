package com.cygnus.ipoten.personality_interview.service;

import com.cygnus.ipoten.personality_interview.entity.PersonalityInterview;
import com.cygnus.ipoten.personality_interview.repository.PersonalityInterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalityInterviewServiceImpl implements PersonalityInterviewService {

    private final PersonalityInterviewRepository personalityInterviewRepository;


    @Override
    public List<PersonalityInterview> getPersonalityInterviews() {

        return personalityInterviewRepository.getPersonalityInterviewInInterview(PageRequest.of(0, 6));

    }
}
