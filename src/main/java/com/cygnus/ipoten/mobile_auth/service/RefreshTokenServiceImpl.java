package com.cygnus.ipoten.mobile_auth.service;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.mobile_auth.entity.AccountRefreshToken;
import com.cygnus.ipoten.mobile_auth.repository.AccountRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final AccountRefreshTokenRepository repository;

    private static final int EXPIRES_DAYS = 30;

    @Override
    @Transactional
    public String createOrReplace(Account account) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expires = LocalDateTime.now().plusDays(EXPIRES_DAYS);

        Optional<AccountRefreshToken> existing = repository.findByAccountId(account.getId());
        if (existing.isPresent()) {
            existing.get().update(token, expires);
            repository.save(existing.get());
        } else {
            repository.save(new AccountRefreshToken(account, token, expires));
        }

        log.info("[RefreshToken] 발급 - accountId: {}", account.getId());
        return token;
    }

    @Override
    public Optional<AccountRefreshToken> validate(String token) {
        return repository.findByRefreshToken(token).filter(AccountRefreshToken::isValid);
    }

    @Override
    @Transactional
    public String rotate(AccountRefreshToken tokenEntity) {
        tokenEntity.revoke();
        repository.save(tokenEntity);
        return createOrReplace(tokenEntity.getAccount());
    }

    @Override
    @Transactional
    public void revoke(String token) {
        repository.findByRefreshToken(token).ifPresent(t -> {
            t.revoke();
            repository.save(t);
            log.info("[RefreshToken] 폐기 - accountId: {}", t.getAccount().getId());
        });
    }

    @Override
    @Transactional
    public void revokeByAccountId(Long accountId) {
        repository.findByAccountId(accountId).ifPresent(t -> {
            t.revoke();
            repository.save(t);
            log.info("[RefreshToken] 계정 기준 폐기 - accountId: {}", accountId);
        });
    }
}
