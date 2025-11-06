package com.cygnus.ipoten.authentication.service;

public interface AuthenticationService {

    String createUserTokenWithAccessToken(Long accountId, String accessToken);
    String createTemporaryUserTokenWithAccessToken(String accessToken);
    boolean deleteToken(String userToken);
    boolean logout(String userToken);
    boolean verification(String currentUserToken);
    Long getAccountIdByUserToken(String userToken);

}
