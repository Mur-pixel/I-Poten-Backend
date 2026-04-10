package com.cygnus.iptn.fcm.service;

public interface FcmNotificationService {
    void sendToAccount(Long accountId, String title, String body, Long interviewId);
}
