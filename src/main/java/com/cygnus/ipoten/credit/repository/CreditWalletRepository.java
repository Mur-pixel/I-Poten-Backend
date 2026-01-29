package com.cygnus.ipoten.credit.repository;


import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.credit.entity.CreditWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditWalletRepository extends JpaRepository<CreditWallet,Long> {
    Optional<CreditWallet> findByAccount(Account account);
}
