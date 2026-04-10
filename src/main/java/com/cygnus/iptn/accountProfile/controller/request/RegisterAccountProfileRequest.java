package com.cygnus.iptn.accountProfile.controller.request;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RegisterAccountProfileRequest {

    private final String nickname;
    private final String email;


}
