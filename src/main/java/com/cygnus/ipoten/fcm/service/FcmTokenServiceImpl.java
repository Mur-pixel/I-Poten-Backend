package com.cygnus.ipoten.fcm.service;

import com.cygnus.ipoten.fcm.entity.AccountFcmToken;
import com.cygnus.ipoten.fcm.repository.AccountFcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FcmTokenServiceImpl implements FcmTokenService {

    private final AccountFcmTokenRepository accountFcmTokenRepository;

    @Override
    @Transactional
    public void registerToken(Long accountId, String token) {
        accountFcmTokenRepository.findByAccountId(accountId)
                .ifPresentOrElse(
                        existing -> existing.updateToken(token),
                        () -> accountFcmTokenRepository.save(new AccountFcmToken(accountId, token))
                );
    }
}
