package com.cygnus.ipoten.google_authentication.service.response;

import com.cygnus.ipoten.google_authentication.service.mobile_response.GoogleLoginMobileResponse;
import com.cygnus.ipoten.kakao_authentication.service.mobile_response.KakaoLoginMobileResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public abstract class GoogleLoginResponse {

    public static GoogleLoginResponse of( boolean isNewUser, String token, String nickname, String email, String origin) {
        return isNewUser
                ? new NewUserGoogleLoginResponse(isNewUser, token, nickname, email, origin)
                : new ExisitingUserGoogleLoginResponse(isNewUser, token, nickname, email, origin);

    }
    public static GoogleLoginMobileResponse ofMobile(boolean isNewUser, String token, String nickname, String email, String origin) {
        return new GoogleLoginMobileResponse(isNewUser, token, nickname, email);
    }


    public abstract String getHtmlResponse();

    public abstract String getUserToken();

    public abstract boolean isNewUser();

    protected static String escape(String str) {return str.replace("'", "\\'");}




}
