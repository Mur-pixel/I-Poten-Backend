package com.cygnus.ipoten.google_authentication.service.mobile_response;


import lombok.Getter;

@Getter
public class GoogleLoginMobileResponse {

    private boolean isNewUser;
    private String token;
    private String nickname;
    private String email;
    private String refreshToken;
    private boolean rejoinUser; // 재가입 대상 여부 : true 이면 과거 탈퇴 이력이 있으나 재가입 가능 조건을 만족한 상태

    public GoogleLoginMobileResponse(boolean isNewUser, String token, String nickname, String email) {
        this(isNewUser, token, nickname, email, null, false);
    }

    public GoogleLoginMobileResponse(boolean isNewUser, String token, String nickname, String email, boolean rejoinUser) {
        this(isNewUser, token, nickname, email, null, rejoinUser);
    }

    public GoogleLoginMobileResponse(boolean isNewUser, String token, String nickname, String email, String refreshToken) {
        this(isNewUser, token, nickname, email, refreshToken, false);
    }

    public GoogleLoginMobileResponse(boolean isNewUser, String token, String nickname, String email, String refreshToken, boolean rejoinUser) {
        this.isNewUser = isNewUser;
        this.token = token;
        this.nickname = nickname;
        this.email = email;
        this.refreshToken = refreshToken;
        this.rejoinUser = rejoinUser;
    }

}
