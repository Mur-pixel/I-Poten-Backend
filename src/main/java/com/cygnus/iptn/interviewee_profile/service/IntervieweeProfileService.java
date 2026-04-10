package com.cygnus.iptn.interviewee_profile.service;

import com.cygnus.iptn.interview.controller.request.IntervieweeProfileRequest;
import com.cygnus.iptn.interviewee_profile.entity.IntervieweeProfile;

import java.util.Optional;

public interface IntervieweeProfileService {

    IntervieweeProfile createIntervieweeProfile(IntervieweeProfileRequest intervieweeProfileRequest);
    Optional<IntervieweeProfile> findById(Long id);


}
