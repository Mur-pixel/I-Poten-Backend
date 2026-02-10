package com.cygnus.ipoten.stt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttService {

    @Value("${FASTAPI_URI}")
    private String fastApiUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public String convertSpeechToText(MultipartFile audioFile) throws Exception {
        try {
            // 1. FastAPI STT 엔드포인트 URL
            String sttUrl = fastApiUri + "stt";
            long startTime = System.currentTimeMillis();

            log.info("[STT] FastAPI 호출 시작 - URL: {}, 파일 크기: {}bytes", sttUrl, audioFile.getSize());

            // 2. Multipart 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 3. 파일을 ByteArrayResource로 변환 (파일명 포함)
            ByteArrayResource fileResource = new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getOriginalFilename();
                }
            };

            // 4. Multipart Body 구성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("audio", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 5. FastAPI로 요청 전송
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    sttUrl,
                    requestEntity,
                    Map.class
            );

            // 6. 응답에서 텍스트 추출
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String text = (String) response.getBody().get("text");

                if (text == null || text.isEmpty()) {
                    log.warn("[STT] 변환 결과가 비어있습니다");
                    return "";
                }

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("[STT] FastAPI 호출 성공 - 소요 시간: {}ms ({}초)", elapsed, elapsed / 1000.0);
                return text;
            } else {
                throw new Exception("FastAPI 응답 오류: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("[STT] FastAPI 호출 실패", e);
            throw new Exception("STT 변환 실패: " + e.getMessage(), e);
        }
    }
}
