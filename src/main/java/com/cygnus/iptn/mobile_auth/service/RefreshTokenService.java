package com.cygnus.iptn.mobile_auth.service;

import com.cygnus.iptn.account.entity.Account;
import com.cygnus.iptn.mobile_auth.entity.AccountRefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    String createOrReplace(Account account);
    Optional<AccountRefreshToken> validate(String token);
    String rotate(AccountRefreshToken tokenEntity);
    void revoke(String token);
    void revokeByAccountId(Long accountId);
}
