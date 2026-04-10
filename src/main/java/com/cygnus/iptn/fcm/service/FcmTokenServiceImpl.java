package com.cygnus.iptn.fcm.service;

import com.cygnus.iptn.fcm.entity.AccountFcmToken;
import com.cygnus.iptn.fcm.repository.AccountFcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
