package com.cygnus.iptn.mobile_auth.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MobileRefreshResponse {
    private String accessToken;
    private String refreshToken;
    private String nickname;
}
