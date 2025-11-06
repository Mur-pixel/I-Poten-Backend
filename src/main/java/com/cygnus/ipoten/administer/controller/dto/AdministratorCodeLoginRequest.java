package com.cygnus.ipoten.administer.controller.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdministratorCodeLoginRequest {
    private String administratorId;
    private String administratorpassword;
}
