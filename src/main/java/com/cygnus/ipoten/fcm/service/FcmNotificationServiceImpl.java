package com.cygnus.ipoten.fcm.service;

import com.cygnus.ipoten.fcm.repository.AccountFcmTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationServiceImpl implements FcmNotificationService {

    private final AccountFcmTokenRepository accountFcmTokenRepository;

    @Override
    public void sendToAccount(Long accountId, String title, String body, Long interviewId) {
        accountFcmTokenRepository.findByAccountId(accountId).ifPresent(fcmToken -> {
            try {
                Message.Builder messageBuilder = Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build());
                if (interviewId != null) {
                    messageBuilder.putData("interviewId", String.valueOf(interviewId));
                }
                String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
                log.info("FCM 전송 성공 - accountId: {}, response: {}", accountId, response);
            } catch (FirebaseMessagingException e) {
                log.error("FCM 전송 실패 - accountId: {}, error: {}", accountId, e.getMessage());
            }
        });
    }
}
