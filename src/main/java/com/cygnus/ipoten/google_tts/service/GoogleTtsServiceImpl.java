package com.cygnus.ipoten.google_tts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class GoogleTtsServiceImpl implements GoogleTtsService {

    @Value("${gcp.tts.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public byte[] synthesize(String text) {

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

        var response = restTemplate.postForEntity(url, body, Map.class);

        if (response.getBody() == null) {
            throw new RuntimeException("Google TTS API 응답이 비어있습니다.");
        }

        Object audioContent = response.getBody().get("audioContent");
        if (audioContent == null) {
            throw new RuntimeException("audioContent가 없습니다. 응답: " + response.getBody());
        }

        // ✅ Base64 → byte[] 변환을 서비스에서 처리
        return Base64.getDecoder().decode((String) audioContent);
    }
}
