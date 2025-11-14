package com.cygnus.ipoten.naver_authentication.service.response;


import lombok.Getter;

@Getter
public abstract class NaverLoginResponse {

    public static NaverLoginResponse of( boolean isNewUser, String token, String nickname, String email, String origin) {
        return isNewUser
                ? new NewUserNaverLoginResponse(isNewUser, token, nickname, email, origin)
                : new ExistingUserNaverLoginResponse(isNewUser, token, nickname, email, origin);
    }

    public abstract String getHtmlResponse();
    public abstract String getUserToken();
    public abstract boolean getIsNewUser();
    protected static String escape(String str) {
        return str.replace("'", "\\'");
    }

}
