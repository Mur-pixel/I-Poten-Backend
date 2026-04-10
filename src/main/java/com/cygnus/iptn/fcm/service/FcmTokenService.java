package com.cygnus.iptn.fcm.service;

public interface FcmTokenService {
    void registerToken(Long accountId, String token);
}
