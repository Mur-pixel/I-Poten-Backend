package com.cygnus.iptn.credit.repository;


import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.credit.entity.CreditWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditWalletRepository extends JpaRepository<CreditWallet,Long> {
    Optional<CreditWallet> findByAccount(Account account);

    @Modifying
    @Query("""
        UPDATE CreditWallet c
        SET c.balance = c.balance - :price
        WHERE c.account.id = :accountId
        AND c.balance >= :price
    """)
    int useCredit(Long accountId, Long price);

    Optional<CreditWallet> findByAccountId(Long accountId);
}
