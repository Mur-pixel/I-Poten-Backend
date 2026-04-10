package com.cygnus.ipoten.personality_interview.controller;

import com.cygnus.ipoten.personality_interview.service.PersonalityInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("personality-interview")
public class PersonalityInterviewController {

    private final PersonalityInterviewService personalityInterviewService;

    @PostMapping
    public ResponseEntity<String> savePersonalityInterviews(@RequestBody List<String> descriptions) {
        personalityInterviewService.saveAll(descriptions);
        return ResponseEntity.ok(descriptions.size() + "건 저장 완료");
    }
}
