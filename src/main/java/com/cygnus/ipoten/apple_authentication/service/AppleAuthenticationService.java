package com.cygnus.ipoten.apple_authentication.service;

import com.cygnus.ipoten.apple_authentication.controller.request.AppleLoginMobileRequest;
import com.cygnus.ipoten.apple_authentication.service.mobile_response.AppleLoginMobileResponse;

public interface AppleAuthenticationService {
    AppleLoginMobileResponse handleLoginMobile(AppleLoginMobileRequest request);
}
