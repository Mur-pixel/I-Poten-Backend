package com.cygnus.iptn.account.service.register_request;


import com.cygnus.iptn.account.entity.LoginType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RegisterAccountRequest {

    private final LoginType loginType;


}
