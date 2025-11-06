package com.cygnus.ipoten.account.controller.request_form;

import com.cygnus.ipoten.account.entity.LoginType;
import com.cygnus.ipoten.account.service.register_request.RegisterAccountRequest;
import com.cygnus.ipoten.accountProfile.controller.request.RegisterAccountProfileRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public class RegisterRequestForm {

    private final String email;
    private final String nickname;
    private final LoginType loginType;


    public RegisterAccountRequest toRegisterAccountRequest() {
        return new RegisterAccountRequest(loginType);
    }

    public RegisterAccountProfileRequest toRegisterAccountProfileRequestForm() {
        return new RegisterAccountProfileRequest(nickname, email);
    }


}
