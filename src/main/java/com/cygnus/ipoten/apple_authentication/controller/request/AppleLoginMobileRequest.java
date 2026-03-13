package com.cygnus.ipoten.apple_authentication.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AppleLoginMobileRequest {
    private String authorizationCode;
    private String identityToken;
    private String email;
    private String givenName;
    private String familyName;
}
