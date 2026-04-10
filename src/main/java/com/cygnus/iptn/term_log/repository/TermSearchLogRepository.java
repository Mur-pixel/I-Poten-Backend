package com.cygnus.iptn.term_log.repository;

import com.cygnus.iptn.term_log.entity.TermSearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TermSearchLogRepository extends JpaRepository<TermSearchLog, Long> {

    @Query("""
        select max(t.createdAt)
        from TermSearchLog t
        where t.actorKey = :actorKey
    """)
    Optional<Instant> findLatestCreatedAtByActorKey(@Param("actorKey") String actorKey);
}
