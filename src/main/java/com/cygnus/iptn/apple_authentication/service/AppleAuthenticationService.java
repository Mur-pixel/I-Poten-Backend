package com.cygnus.iptn.apple_authentication.service;

import com.cygnus.iptn.apple_authentication.controller.request.AppleLoginMobileRequest;
import com.cygnus.iptn.apple_authentication.service.mobile_response.AppleLoginMobileResponse;

public interface AppleAuthenticationService {
    AppleLoginMobileResponse handleLoginMobile(AppleLoginMobileRequest request);
}
