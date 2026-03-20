package com.cygnus.ipoten.interest.repository;

import com.cygnus.ipoten.interest.entity.AccountInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountInterestRepository extends JpaRepository<AccountInterest, Long> {
    List<AccountInterest> findByAccountId(Long accountId);

    void deleteByAccountId(Long accountId);
}
