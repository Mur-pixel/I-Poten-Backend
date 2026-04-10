package com.cygnus.iptn.account.service.register_response;


import lombok.Getter;

@Getter
public class RegisterResponse {

    private String nickname;
    private String email;
    private String userToken;

    public RegisterResponse() {
    }

    public RegisterResponse(String nickname, String email, String userToken) {
        this.nickname = nickname;
        this.email = email;
        this.userToken = userToken;
    }
}
