package com.cygnus.ipoten.stt.controller;

import com.cygnus.ipoten.stt.service.SttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/stt")
@RequiredArgsConstructor
public class SttController {

    private final SttService sttService;

    @PostMapping("/convert")
    public ResponseEntity<String> convertSpeechToText(
            @RequestParam("audio") MultipartFile audioFile
    ) {
        try {
            log.info("[STT] 음성 변환 요청 - 파일명: {}, 크기: {} bytes",
                    audioFile.getOriginalFilename(),
                    audioFile.getSize());

            String text = sttService.convertSpeechToText(audioFile);

            log.info("[STT] 변환 성공 - 텍스트 길이: {} 자", text.length());

            return ResponseEntity.ok(text);

        } catch (Exception e) {
            log.error("[STT] 변환 실패", e);
            return ResponseEntity.internalServerError()
                    .body("음성 변환 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
