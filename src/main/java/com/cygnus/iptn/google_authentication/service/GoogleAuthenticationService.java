package com.cygnus.iptn.google_authentication.service;

import com.cygnus.iptn.google_authentication.service.mobile_response.GoogleLoginMobileResponse;
import com.cygnus.iptn.google_authentication.service.response.GoogleLoginResponse;
import com.cygnus.iptn.kakao_authentication.service.mobile_response.KakaoLoginMobileResponse;

import java.util.Map;

public interface GoogleAuthenticationService {

    String Link();

    GoogleLoginResponse handleLogin(String code);

    String getAccessToken(String code);

    Map<String, Object> getUserInfo(String accessToken);

    GoogleLoginMobileResponse handleLoginMobile(String accessToken);

}
