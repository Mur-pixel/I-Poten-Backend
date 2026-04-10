package com.cygnus.ipoten.fcm.service;

public interface FcmTokenService {
    void registerToken(Long accountId, String token);
}
