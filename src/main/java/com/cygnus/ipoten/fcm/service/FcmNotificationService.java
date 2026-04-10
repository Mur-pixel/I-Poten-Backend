package com.cygnus.ipoten.fcm.service;

public interface FcmNotificationService {
    void sendToAccount(Long accountId, String title, String body, Long interviewId);
}
