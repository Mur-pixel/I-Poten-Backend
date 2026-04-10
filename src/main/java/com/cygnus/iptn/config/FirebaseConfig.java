package com.cygnus.iptn.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${FIREBASE_ADMIN_CREDENTIALS}")
    private String firebaseCredentials;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        
        // 환경 변수에서 직접 읽기 (파일 형식 문제 없음)
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(firebaseCredentials.getBytes())) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            log.info("firebaseApp 초기화 부분");
            return FirebaseApp.initializeApp(options);
        }
    }
}
