package com.cygnus.iptn.personality_interview.service;

import com.cygnus.iptn.personality_interview.entity.PersonalityInterview;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PersonalityInterviewService {

    List<PersonalityInterview> getPersonalityInterviews();

    void saveAll(List<String> descriptions);


}
