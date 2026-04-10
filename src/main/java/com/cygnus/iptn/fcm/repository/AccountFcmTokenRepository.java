package com.cygnus.iptn.fcm.repository;

import com.cygnus.iptn.fcm.entity.AccountFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountFcmTokenRepository extends JpaRepository<AccountFcmToken, Long> {
    Optional<AccountFcmToken> findByAccountId(Long accountId);
}
