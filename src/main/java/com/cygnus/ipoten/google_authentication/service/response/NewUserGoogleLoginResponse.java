package com.cygnus.ipoten.google_authentication.service.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class NewUserGoogleLoginResponse extends GoogleLoginResponse {

    private final String htmlResponse;
    private final String userToken;
    private final boolean isNewUser;

    public NewUserGoogleLoginResponse(boolean isNewUser, String token, String nickname, String email, String origin) {
        this.isNewUser = isNewUser;
        this.userToken = token;
        this.htmlResponse = """
        <html><body><script>
        window.opener.postMessage({
            isNewUser: %s,
            accessToken: '%s',
            user: { nickname: '%s', email: '%s' }
        }, '%s'); window.close();
        </script></body></html>
        """.formatted(isNewUser, token, escape(nickname), escape(email), origin);
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
    public boolean isNewUser() {
        return isNewUser;
    }


}
