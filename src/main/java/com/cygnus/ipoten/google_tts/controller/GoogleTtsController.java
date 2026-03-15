package com.cygnus.ipoten.google_tts.controller;

import com.cygnus.ipoten.google_tts.controller.request_form.TtsRequestForm;
import com.cygnus.ipoten.google_tts.service.GoogleTtsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequiredArgsConstructor
@RequestMapping("google_tts")
public class GoogleTtsController {

    private final GoogleTtsService googleTtsService;

    @PostMapping
    public String ttsTest(@RequestBody TtsRequestForm req) {
            return googleTtsService.synthesizeAndUpload(req.getText());

    }

}
