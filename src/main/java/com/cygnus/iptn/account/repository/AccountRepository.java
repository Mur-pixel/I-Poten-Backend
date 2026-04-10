package com.cygnus.iptn.account.repository;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.account.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface AccountRepository extends JpaRepository<Account, Long> {

    long countByStatus(AccountStatus status);

    long countByCreatedAtAfter(Instant after);
}
