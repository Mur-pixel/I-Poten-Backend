package com.cygnus.ipoten.resource;

public class SessionAuthService {
    public void verifySession(String token) {
        try {
            Thread.sleep(3); // Redis I/O 시뮬레이션
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}