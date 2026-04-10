package com.cygnus.ipoten.google_authentication.service.response;

public class ExisitingUserGoogleLoginResponse extends GoogleLoginResponse {

    private final String htmlResponse;
    private final String userToken;
    private final boolean isNewUser;
    private final boolean rejoinUser = false;

    public ExisitingUserGoogleLoginResponse(boolean isNewUser, String token, String nickname, String email, String origin) {
        this.isNewUser = isNewUser;
        this.userToken = token;
        this.htmlResponse = """
        <html><body><script>
        window.opener.postMessage({
            isNewUser: %s,
            rejoinUser: false,
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
