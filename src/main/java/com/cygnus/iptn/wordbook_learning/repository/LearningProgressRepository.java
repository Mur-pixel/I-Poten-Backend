package com.cygnus.iptn.wordbook_learning.repository;

import com.cygnus.iptn.wordbook_learning.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, LearningProgress.Id> {
    List<LearningProgress> findByIdAccountIdAndIdTermIdIn(Long accountId, Collection<Long> termIds);
    Optional<LearningProgress> findByIdAccountIdAndIdTermId(Long accountId, Long termId);

    @Query(value = """
        select max(greatest(
            coalesce(lp.updated_at, cast('1970-01-01 00:00:00' as datetime)),
            coalesce(lp.last_studied_at, cast('1970-01-01 00:00:00' as datetime)),
            coalesce(lp.completed_at, cast('1970-01-01 00:00:00' as datetime))
        ))
        from learning_progress lp
        where lp.account_id = :accountId
    """, nativeQuery = true)
    Optional<Instant> findLatestActivityAtByAccountId(@Param("accountId") Long accountId);
}
