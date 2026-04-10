package com.cygnus.ipoten.mobile_auth.repository;

import com.cygnus.ipoten.mobile_auth.entity.AccountRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRefreshTokenRepository extends JpaRepository<AccountRefreshToken, Long> {
    @Query("SELECT t FROM AccountRefreshToken t JOIN FETCH t.account WHERE t.refreshToken = :token")
    Optional<AccountRefreshToken> findByRefreshToken(@Param("token") String token);
    Optional<AccountRefreshToken> findByAccountId(Long accountId);
}
