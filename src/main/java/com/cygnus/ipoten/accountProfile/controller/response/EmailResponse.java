package com.cygnus.ipoten.accountProfile.controller.response;

import lombok.Getter;

@Getter
public class EmailResponse {

    private String email;

    public EmailResponse(String email) {
        this.email = email;
    }
}
