package com.cygnus.ipoten.interviewee_profile.service;

import com.cygnus.ipoten.interview.controller.request.IntervieweeProfileRequest;
import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;

import java.util.Optional;

public interface IntervieweeProfileService {

    IntervieweeProfile createIntervieweeProfile(IntervieweeProfileRequest intervieweeProfileRequest);
    Optional<IntervieweeProfile> findById(Long id);


}
