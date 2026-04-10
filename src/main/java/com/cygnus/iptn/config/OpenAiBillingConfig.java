// src/main/java/com/wowraid/jobspoon/config/OpenAiBillingConfig.java
package com.cygnus.iptn.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * OpenAI Admin API 호출용 RestClient 구성 (JDK HttpClient 기반).
 * - 외부 라이브러리 불필요(JDK 11+)
 * - 공통 Authorization 헤더 주입
 * - 연결/읽기 타임아웃 기본값 설정
 */
@Slf4j
@Configuration
public class OpenAiBillingConfig {

    @Bean
    public RestClient openAiAdminClient(@Value("${openai.api.admin-key}") String adminKey) {
        // JDK HttpClient: 연결 타임아웃
        HttpClient jdkHttp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Spring request factory: 읽기 타임아웃
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkHttp);
        factory.setReadTimeout(Duration.ofSeconds(15));

        // 모든 요청에 공통 헤더 삽입(Authorization, Accept)
        ClientHttpRequestInterceptor auth = (req, body, ex) -> {
            var h = req.getHeaders();
            h.set(HttpHeaders.AUTHORIZATION, "Bearer " + adminKey);
            h.setAccept(List.of(MediaType.APPLICATION_JSON));
//            log.info("→ {} {}", req.getMethod(), req.getURI());
//            log.info("→ Authorization: Bearer {}****", adminKey.substring(0,6));
            return ex.execute(req, body);
        };

        return RestClient.builder()
                .baseUrl("https://api.openai.com/")
                .requestFactory(factory)
                .requestInterceptor(auth)
                .build();
    }
}