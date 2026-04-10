package com.cygnus.iptn.kakao_authentication.service.mobile_response;


import lombok.Getter;

@Getter
public class KakaoLoginMobileResponse {

    private boolean isNewUser;
    private String token;
    private String nickname;
    private String email;
    private String refreshToken;
    private boolean rejoinUser;

    public KakaoLoginMobileResponse(boolean isNewUser, String token, String nickname, String email) {
        this(isNewUser, token, nickname, email, null, false);
    }

    public KakaoLoginMobileResponse(boolean isNewUser, String token, String nickname, String email, boolean rejoinUser) {
        this(isNewUser, token, nickname, email, null, rejoinUser);
    }

    public KakaoLoginMobileResponse(boolean isNewUser, String token, String nickname, String email, String refreshToken) {
        this(isNewUser, token, nickname, email, refreshToken, false);
    }

    public KakaoLoginMobileResponse(boolean isNewUser, String token, String nickname, String email, String refreshToken, boolean rejoinUser) {
        this.isNewUser = isNewUser;
        this.token = token;
        this.nickname = nickname;
        this.email = email;
        this.refreshToken = refreshToken;
        this.rejoinUser = rejoinUser;
    }

}
