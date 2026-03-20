package com.cygnus.ipoten.interviewee_profile.repository;

import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IntervieweeProfileRepository extends JpaRepository<IntervieweeProfile, Long> {



}
