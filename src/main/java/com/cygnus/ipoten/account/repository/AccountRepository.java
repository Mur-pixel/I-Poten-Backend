package com.cygnus.ipoten.account.repository;

import com.cygnus.ipoten.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

}
