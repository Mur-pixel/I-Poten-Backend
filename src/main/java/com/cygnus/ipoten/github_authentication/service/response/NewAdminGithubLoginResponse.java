package com.cygnus.ipoten.github_authentication.service.response;


import lombok.Getter;

@Getter
public class NewAdminGithubLoginResponse extends GithubLoginResponse {

    private final String htmlResponse;
    private final String userToken;

    public NewAdminGithubLoginResponse(boolean isNewUser, String Token, String nickname, String email,String origin) {
        this.userToken = Token;
        this.htmlResponse = """
        <html><body><script>
        window.opener.postMessage({
            isNewUser: %s,
            accessToken: '%s',
            user: { nickname: '%s', email: '%s' }
        }, '%s'); window.close();
        </script></body></html>
        """.formatted(isNewUser, userToken, escape(nickname), escape(email), origin);
    }


    @Override
    public String getHtmlResponse() {
        return htmlResponse;
    }
    @Override
    public String getUserToken() {
        return userToken;
    }
}
