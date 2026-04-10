package com.cygnus.iptn.mobile_auth.controller.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MobileRefreshRequest {
    private String refreshToken;
}
