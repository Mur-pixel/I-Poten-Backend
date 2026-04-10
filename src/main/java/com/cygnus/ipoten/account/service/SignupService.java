package com.cygnus.ipoten.account.service;

import com.cygnus.ipoten.account.controller.request_form.RegisterRequestForm;
import com.cygnus.ipoten.account.service.register_response.RegisterResponse;

public interface SignupService {

    RegisterResponse signup(String AccessToken, RegisterRequestForm registerRequestForm);

}
