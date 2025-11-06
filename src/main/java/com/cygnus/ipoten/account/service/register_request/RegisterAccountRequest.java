package com.cygnus.ipoten.account.service.register_request;


import com.cygnus.ipoten.account.entity.LoginType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RegisterAccountRequest {

    private final LoginType loginType;


}
