package com.cygnus.ipoten.naver_authentication.service.response;


import com.cygnus.ipoten.meta_authentication.service.response.MetaLoginResponse;
import lombok.Getter;

@Getter
public class NewUserNaverLoginResponse extends NaverLoginResponse {

    private final String htmlResponse;
    private final String userToken;
    private final boolean isNewUser;
    private final boolean rejoinUser;


    public NewUserNaverLoginResponse(
            boolean isNewUser,
            boolean rejoinUser,
            String token,
            String nickname,
            String email,
            String origin
    ) {
        this.isNewUser = isNewUser;
        this.rejoinUser = rejoinUser;
        this.userToken = token;
        this.htmlResponse = """
        <html><body><script>
        window.opener.postMessage({
            isNewUser: %s,
            rejoinUser: %s,
            accessToken: '%s',
            user: { nickname: '%s', email: '%s' }
        }, '%s'); window.close();
        </script></body></html>
        """.formatted(isNewUser, rejoinUser, token, escape(nickname), escape(email), origin);
    }

    @Override
    public String getHtmlResponse() {
        return htmlResponse;
    }

    @Override
    public String getUserToken() {
        return userToken;
    }

    @Override
    public boolean getIsNewUser() {
        return isNewUser;
    }

}
