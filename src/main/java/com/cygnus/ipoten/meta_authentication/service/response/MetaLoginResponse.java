package com.cygnus.ipoten.meta_authentication.service.response;

import com.cygnus.ipoten.kakao_authentication.service.mobile_response.KakaoLoginMobileResponse;
import lombok.Getter;

@Getter
public abstract class MetaLoginResponse {



    public static MetaLoginResponse of(boolean isNewUser, String token, String nickname, String email, String origin) {
        return isNewUser
                ? new NewUserMetaLoginResponse(isNewUser, token, nickname, email, origin)
                : new ExistingUserMetaLoginResponse(isNewUser, token, nickname, email, origin);
    }

    public static KakaoLoginMobileResponse ofMobile(boolean isNewUser, String token, String nickname, String email, String origin) {
        return new KakaoLoginMobileResponse(isNewUser, token, nickname, email);
    }



    public abstract String getHtmlResponse();
    public abstract String getUserToken();
    public abstract boolean getIsNewUser();
    protected static String escape(String str) {
        return str.replace("'", "\\'");
    }

}

