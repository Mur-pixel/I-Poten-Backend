package com.cygnus.iptn.personality_interview.repository;

import com.cygnus.iptn.personality_interview.entity.PersonalityInterview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface PersonalityInterviewRepository extends JpaRepository<PersonalityInterview, Long> {

    @Query("""
    SELECT p
    FROM PersonalityInterview p
    ORDER BY function('rand')
    """)
    List<PersonalityInterview> getPersonalityInterviewInInterview(Pageable pageable);



}
