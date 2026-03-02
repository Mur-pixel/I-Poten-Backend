package com.cygnus.ipoten.mobile_auth.repository;

import com.cygnus.ipoten.mobile_auth.entity.AccountRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRefreshTokenRepository extends JpaRepository<AccountRefreshToken, Long> {
    Optional<AccountRefreshToken> findByRefreshToken(String refreshToken);
    Optional<AccountRefreshToken> findByAccountId(Long accountId);
}
