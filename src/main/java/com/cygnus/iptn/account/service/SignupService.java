package com.cygnus.iptn.account.service;

import com.cygnus.iptn.account.controller.request_form.RegisterRequestForm;
import com.cygnus.iptn.account.service.register_response.RegisterResponse;

public interface SignupService {

    RegisterResponse signup(String AccessToken, RegisterRequestForm registerRequestForm);

}
