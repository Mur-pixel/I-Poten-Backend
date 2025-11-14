package com.cygnus.ipoten.naver_authentication.service;

import com.cygnus.ipoten.naver_authentication.service.response.NaverLoginResponse;

import java.util.Map;

public interface NaverAuthenticationService {

    String link();

    NaverLoginResponse handleLogin(String code);

    String getAccessToken(String code);

    Map<String, Object> getUserInfo(String accessToken);



}
