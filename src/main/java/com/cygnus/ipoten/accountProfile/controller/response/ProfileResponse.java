package com.cygnus.ipoten.accountProfile.controller.response;

import lombok.Getter;

@Getter
public class ProfileResponse {

    private String email;
    private String nickname;

    public ProfileResponse(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
    }
}
