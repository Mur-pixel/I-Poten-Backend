//package com.cygnus.ipoten.google_tts.controller;
//
//import com.cygnus.ipoten.google_tts.controller.request_form.TtsRequestForm;
//import com.cygnus.ipoten.google_tts.service.GoogleTtsService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Base64;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("google_tts")
//public class GoogleTtsController {
//
//    private final GoogleTtsService googleTtsService;
//
//    @PostMapping
//    public ResponseEntity<byte[]> ttsTest(@RequestBody TtsRequestForm req){
//        try {
//            String audioBase64 = googleTtsService.synthesize(req.getText());
//
//            // 3) base64 → binary 변환
//            byte[] audioBytes = Base64.getDecoder().decode(audioBase64);
//
//            // 4) 파일 저장 없이 스트림 반환
//            return ResponseEntity.ok()
//                    .header("Content-Type", "audio/mpeg")
//                    .header("Cache-Control", "no-store")
//                    .body(audioBytes);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(500)
//                    .header("Content-Type", "text/plain")
//                    .body(("서버 내부 오류가 발생했습니다: " + e.getMessage()).getBytes());
//        }
//    }
//
//}
