package com.cygnus.ipoten.google_tts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class GoogleTtsServiceImpl implements GoogleTtsService {

    @Value("${gcp.tts.api-key}")
    private String apiKey;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${cdn.base-url}") // https://cdn.myservice.com
    private String cdnBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final S3Client s3Client;

    public GoogleTtsServiceImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String synthesizeAndUpload(String text) {

        Map<String, Object> body = Map.of(
                "input", Map.of("text", text),
                "voice", Map.of(
                        "languageCode", "ko-KR",
                        "name", "en-US-Wavenet-D"
                ),
                "audioConfig", Map.of(
                        "audioEncoding", "MP3"
                )
        );

        String url = "https://texttospeech.googleapis.com/v1/text:synthesize?key=" + apiKey;
        var response = restTemplate.postForEntity(url, body, Map.class);

        if (response.getBody() == null || response.getBody().get("audioContent") == null) {
            throw new RuntimeException("Google TTS API 응답 오류");
        }

        byte[] audioBytes = Base64.getDecoder()
                .decode((String) response.getBody().get("audioContent"));

        // 🔑 S3 key 설계 (버전 필수)
        String key = "questions/v1/" + UUID.randomUUID() + ".mp3";

        // 🔼 S3 업로드
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("audio/mpeg")
                        .cacheControl("public, max-age=86400") // CDN 캐시
                        .build(),
                RequestBody.fromBytes(audioBytes)
        );

        // ✅ CloudFront URL 반환
        return cdnBaseUrl + "/" + key;
    }
}