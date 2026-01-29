package com.cygnus.ipoten.google_tts.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Map;

@Service
public class GoogleTtsServiceImpl implements GoogleTtsService {

    @Value("${gcp.tts.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String synthesize(String text) {
        Map<String, Object> body = Map.of(
                "input", Map.of("text", text),

                "voice", Map.of(
                        "languageCode", "ko-KR",
                        "name", "gemini-2.5-pro-tts"
                ),

                "audioConfig", Map.of(
                        "audioEncoding", "MP3"
                )
        );

            String url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=" + apiKey;

            System.out.println("TTS API 호출 시작 - URL: " + url);
            System.out.println("요청 텍스트: " + text);

            var response = restTemplate.postForEntity(url, body, Map.class);

            System.out.println("TTS API 응답 상태: " + response.getStatusCode());

            if (response.getBody() == null) {
                throw new RuntimeException("Google TTS API 응답이 비어있습니다.");
            }

            Object audioContent = response.getBody().get("audioContent");
            if (audioContent == null) {
                throw new RuntimeException("Google TTS API 응답에 audioContent가 없습니다. 응답: " + response.getBody());
            }

            return (String) audioContent;

    }
}
