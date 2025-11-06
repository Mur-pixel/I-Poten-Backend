package com.cygnus.ipoten.administer.controller;

import com.cygnus.ipoten.administer.controller.dto.AdministratorCodeLoginRequest;
import com.cygnus.ipoten.administer.service.AdministratorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/administrator/authentication")
@RequiredArgsConstructor
public class AdministratorLoginController {
    private final AdministratorService administratorService;

    @GetMapping("/temporaryAdminToken_valid")
    public ResponseEntity<Void> validate(HttpServletRequest req,
                                         @CookieValue(name = "temporaryAdminToken", required = false) String temporaryAdminToken) {
        log.info("Cookie header = {}", req.getHeader("Cookie"));
        log.info("validate is called, tempToken: {}", temporaryAdminToken);
        if(temporaryAdminToken == null || temporaryAdminToken.isEmpty()){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();}
        boolean validresult=administratorService.isTempTokenValid(temporaryAdminToken);
        return validresult
                ? ResponseEntity.ok().build()   // 200
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401
    }

    @PostMapping("/code_login")
    public void code_login
            (@RequestBody AdministratorCodeLoginRequest request, HttpServletResponse response){
        boolean valid = administratorService.validateKey(request.getAdministratorId(), request.getAdministratorpassword());
        if (!valid) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        String temporaryAdminToken = administratorService.createTemporaryAdminToken();
        log.info("[AdministratorLoginController] is called , temporaryAdminToken:{}",temporaryAdminToken);
        String cookieHeader = String.format(
//                "temporaryAdminToken=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=None",
                "temporaryAdminToken=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",

                temporaryAdminToken, 1 * 60
        );
        response.addHeader("Set-Cookie", cookieHeader);
        response.setStatus(HttpStatus.OK.value()); // 204
    }

    @PostMapping("/social_login")
    public ResponseEntity<Void> social_login(@CookieValue(name = "userToken", required = false) String userToken){
//        log.info("[AdministratorController] social_login userToken received: {}", userToken);
        boolean valid = administratorService.isAdminByUserToken(userToken);
//        log.info("[AdministratorController] social_login valid: {}", valid);
        return valid
                ? ResponseEntity.ok().build() //200처리
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); //401 처리
    }
}
