package com.cygnus.iptn.google_authentication.service.response;

import com.cygnus.iptn.google_authentication.service.mobile_response.GoogleLoginMobileResponse;
import com.cygnus.iptn.kakao_authentication.service.mobile_response.KakaoLoginMobileResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public abstract class GoogleLoginResponse {

    public static GoogleLoginResponse of(
            boolean isNewUser,
            boolean isRejoinUser,
            String token,
            String nickname,
            String email,
            String origin
    ) {
        return isNewUser
                ? new NewUserGoogleLoginResponse(isNewUser, isRejoinUser, token, nickname, email, origin)
                : new ExisitingUserGoogleLoginResponse(isNewUser, token, nickname, email, origin);

    }
    public static GoogleLoginMobileResponse ofMobile(
            boolean isNewUser,
            boolean isRejoinUser,
            String token,
            String nickname,
            String email,
            String origin
    ) {
        return new GoogleLoginMobileResponse(isNewUser, token, nickname, email, isRejoinUser);
    }


    public abstract String getHtmlResponse();

    public abstract String getUserToken();

    public abstract boolean isNewUser();

    protected static String escape(String str) {return str.replace("'", "\\'");}




}
