package com.cygnus.iptn.infrastructure.external.email;

public interface EmailService {
    void sendInterviewResultNotification(String to, Long interviewId);
    void sendErrorNotification(String to, Long userToken);

    void sendSignupWelcomeEmail(String to, String nickname);
    void sendWithdrawalConfirmationEmail(String to, String nickname);
}