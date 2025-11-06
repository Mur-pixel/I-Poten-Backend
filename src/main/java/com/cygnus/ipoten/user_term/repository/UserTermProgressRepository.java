package com.cygnus.ipoten.user_term.repository;

import com.cygnus.ipoten.user_term.entity.UserTermProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserTermProgressRepository extends JpaRepository<UserTermProgress, UserTermProgress.Id> {
    List<UserTermProgress> findByIdAccountIdAndIdTermIdIn(Long accountId, Collection<Long> termIds);
    Optional<UserTermProgress> findByIdAccountIdAndIdTermId(Long accountId, Long termId);
}