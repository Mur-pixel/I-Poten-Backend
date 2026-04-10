package com.cygnus.ipoten.account.repository;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface AccountRepository extends JpaRepository<Account, Long> {

    long countByStatus(AccountStatus status);

    long countByCreatedAtAfter(Instant after);
}
