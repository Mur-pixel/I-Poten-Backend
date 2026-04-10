package com.cygnus.ipoten.github_authentication.service.response;


public class ExistingAdminGithubLoginResponse extends GithubLoginResponse {

    private final String htmlResponse;
    private final String userToken;
    public ExistingAdminGithubLoginResponse(boolean isNewUser, String token, String nickname, String email,String origin) {
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
}
