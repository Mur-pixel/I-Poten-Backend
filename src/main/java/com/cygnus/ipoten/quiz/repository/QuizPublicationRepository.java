package com.cygnus.ipoten.quiz.repository;

import com.cygnus.ipoten.quiz.entity.QuizPublication;
import com.cygnus.ipoten.quiz.entity.enums.JobRole;
import com.cygnus.ipoten.quiz.entity.enums.QuizPartType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

public interface QuizPublicationRepository extends JpaRepository<QuizPublication, Long> {
    Optional<QuizPublication> findFirstByScheduledDateAndPartTypeAndJobRoleAndActiveIsTrue(
            LocalDate date, QuizPartType part, JobRole role);

    // 날짜 = 오늘, part 일치, role ∈ {요청 role, GENERAL}, 또는 role IS NULL
    @Query("""
        select p from QuizPublication p
        where p.active = true
          and p.partType = :part
          and p.scheduledDate = :date
          and (p.jobRole in :roles or p.jobRole is null)
        order by p.id desc
    """)
    Optional<QuizPublication> findExact(
            @Param("date") LocalDate date,
            @Param("part") QuizPartType part,
            @Param("roles") Collection<JobRole> roles
    );

    // 날짜 ≤ 오늘 중 가장 최근, part 일치, role ∈ {요청 role, GENERAL}, 또는 role IS NULL
    @Query("""
        select p from QuizPublication p
        where p.active = true
          and p.partType = :part
          and p.scheduledDate <= :date
          and (p.jobRole in :roles or p.jobRole is null)
        order by p.scheduledDate desc, p.id desc
    """)
    Optional<QuizPublication> findLatestOnOrBefore(
            @Param("date") LocalDate date,
            @Param("part") QuizPartType part,
            @Param("roles") Collection<JobRole> roles
    );
}

