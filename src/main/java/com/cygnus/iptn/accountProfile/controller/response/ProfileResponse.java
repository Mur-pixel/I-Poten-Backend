package com.cygnus.iptn.accountProfile.controller.response;

import lombok.Getter;

import java.time.Instant;

@Getter
public class ProfileResponse {

    private String email;
    private String nickname;
    private Instant lastActivityAt;
    private String representativeLabel;
    private String representativeJob;
    private String representativeCareer;

    public ProfileResponse(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }

    public ProfileResponse(String email, String nickname, Instant lastActivityAt) {
        this.email = email;
        this.nickname = nickname;
        this.lastActivityAt = lastActivityAt;
    }

    public ProfileResponse(
            String email,
            String nickname,
            Instant lastActivityAt,
            String representativeLabel,
            String representativeJob,
            String representativeCareer
    ) {
        this.email = email;
        this.nickname = nickname;
        this.lastActivityAt = lastActivityAt;
        this.representativeLabel = representativeLabel;
        this.representativeJob = representativeJob;
        this.representativeCareer = representativeCareer;
    }
}
