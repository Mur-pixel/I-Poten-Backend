package com.cygnus.iptn.accountProfile.controller.response;


import lombok.Getter;

@Getter
public class NicknameResponse {

    private String nickname;

    public NicknameResponse(String nickname) {
        this.nickname = nickname;
    }
}
