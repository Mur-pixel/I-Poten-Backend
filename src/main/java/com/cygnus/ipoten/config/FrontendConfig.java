package com.cygnus.ipoten.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Configuration
@ConfigurationProperties(prefix = "frontend.authentication")
public class FrontendConfig {
    private List<String> origins;

    public void setOrigins(List<String> origins) {
        this.origins = origins;
    }
}


