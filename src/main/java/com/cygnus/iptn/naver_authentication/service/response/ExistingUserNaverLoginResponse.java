package com.cygnus.iptn.naver_authentication.service.response;


public class ExistingUserNaverLoginResponse extends NaverLoginResponse {

    private final String htmlResponse;
    private final String userToken;
    private final boolean isNewUser;
    private final boolean rejoinUser = false;

    public ExistingUserNaverLoginResponse(boolean isNewUser, String token, String nickname, String email, String origin) {
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
    public boolean getIsNewUser() {
        return isNewUser;
    }
}
