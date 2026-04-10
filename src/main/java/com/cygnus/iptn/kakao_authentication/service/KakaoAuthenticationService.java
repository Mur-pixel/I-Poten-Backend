package com.cygnus.iptn.kakao_authentication.service;

import com.cygnus.iptn.kakao_authentication.service.mobile_response.KakaoLoginMobileResponse;
import com.cygnus.iptn.kakao_authentication.service.response.KakaoLoginResponse;

import java.util.Map;

public interface KakaoAuthenticationService {
    String requestKakaoOauthLink();
    Map<String, Object> getAccessToken(String code);
    Map<String, Object> getUserInfo(String accessToken);
    KakaoLoginResponse handleLogin(String code);
    String extractNickname(Map<String, Object> userInfo);
    String extractEmail(Map<String, Object> userInfo);
    KakaoLoginMobileResponse handleLoginMobile(String accessToken);


}

