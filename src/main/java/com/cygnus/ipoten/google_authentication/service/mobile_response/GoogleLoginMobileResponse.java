package com.cygnus.ipoten.google_authentication.service.mobile_response;


import lombok.Getter;

@Getter
public class GoogleLoginMobileResponse {

    private boolean isNewUser;
    private String token;
    private String nickname;
    private String email;
    private String refreshToken;

    public GoogleLoginMobileResponse(boolean isNewUser, String token, String nickname, String email) {
        this.isNewUser = isNewUser;
        this.token = token;
        this.nickname = nickname;
        this.email = email;
    }

    public GoogleLoginMobileResponse(boolean isNewUser, String token, String nickname, String email, String refreshToken) {
        this.isNewUser = isNewUser;
        this.token = token;
        this.nickname = nickname;
        this.email = email;
        this.refreshToken = refreshToken;
    }

}
