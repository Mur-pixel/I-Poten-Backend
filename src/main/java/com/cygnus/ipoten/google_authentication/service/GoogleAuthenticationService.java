package com.cygnus.ipoten.google_authentication.service;

import com.cygnus.ipoten.google_authentication.service.response.GoogleLoginResponse;

import java.util.Map;

public interface GoogleAuthenticationService {

    String Link();

    GoogleLoginResponse handleLogin(String code);

    String getAccessToken(String code);

    Map<String, Object> getUserInfo(String accessToken);

}
