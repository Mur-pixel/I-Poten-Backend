package com.cygnus.iptn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestApiConfig {

    @Value("${internal.api.key:}")
    private String internalApiKey;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            if (!internalApiKey.isBlank()) {
                request.getHeaders().set("X-Internal-Key", internalApiKey);
            }
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(List.of(interceptor));
        return restTemplate;
    }
}
