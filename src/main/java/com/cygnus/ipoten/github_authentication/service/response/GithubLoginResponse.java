package com.cygnus.ipoten.github_authentication.service.response;

public abstract class GithubLoginResponse {
    public static GithubLoginResponse of(boolean isNewUser, String token, String nickname, String email,String origin) {
        return isNewUser
                ? new NewAdminGithubLoginResponse(isNewUser, token, nickname, email, origin)
                : new ExistingAdminGithubLoginResponse(isNewUser, token, nickname, email, origin);
    }
    public abstract String getHtmlResponse();
    public abstract String getUserToken();
    protected static String escape(String str) {
        return str.replace("'", "\\'");
    }

}

