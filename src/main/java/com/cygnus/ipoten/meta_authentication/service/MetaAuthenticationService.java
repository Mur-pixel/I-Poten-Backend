package com.cygnus.ipoten.meta_authentication.service;

import com.cygnus.ipoten.meta_authentication.service.response.MetaLoginResponse;

import java.util.Map;

public interface MetaAuthenticationService {

    String requestKakaoOauthLink();
    MetaLoginResponse handleLogin(String code);
    String getAccessToken(String code);
    Map<String, Object> getUserInfo(String accessToken);

}
