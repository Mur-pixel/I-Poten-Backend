package com.cygnus.ipoten.google_authentication.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GoogleAuthenticationServiceImpl implements GoogleAuthenticationService {

    private final String clientId;
    private final String redirectUri;

    public GoogleAuthenticationServiceImpl(
            @Value("${google.client-id}") String clientId,
            @Value("${google.rediret-uri}") String redirectUri
    ) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
    }




    @Override
    public String Link() {
        return String.format("https://accounts.google.com/o/oauth2/v2/auth?"
                        + "client_id=%s"
                        + "&redirect_uri=%s"
                        + "&response_type=code",
                clientId, redirectUri);
    }
}
