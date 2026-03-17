package com.cygnus.ipoten.personality_interview.service;

import com.cygnus.ipoten.personality_interview.entity.PersonalityInterview;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PersonalityInterviewService {

    List<PersonalityInterview> getPersonalityInterviews();

    void saveAll(List<String> descriptions);


}
