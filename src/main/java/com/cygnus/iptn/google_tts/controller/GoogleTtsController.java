package com.cygnus.iptn.google_tts.controller;

import com.cygnus.iptn.google_tts.controller.request_form.TtsRequestForm;
import com.cygnus.iptn.google_tts.service.GoogleTtsService;
import com.cygnus.iptn.personality_interview.entity.PersonalityInterview;
import com.cygnus.iptn.personality_interview.entity.PersonalityInterviewAudio;
import com.cygnus.iptn.personality_interview.repository.PersonalityInterviewAudioRepository;
import com.cygnus.iptn.personality_interview.repository.PersonalityInterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("google_tts")
public class GoogleTtsController {

    private final GoogleTtsService googleTtsService;
    private final PersonalityInterviewRepository personalityInterviewRepository;
    private final PersonalityInterviewAudioRepository personalityInterviewAudioRepository;

    @PostMapping
    public String ttsTest(@RequestBody TtsRequestForm req) {
            return googleTtsService.synthesizeAndUpload(req.getText());
    }


    @Transactional
    @PostMapping("/personality-interview")
    public ResponseEntity<String> generatePersonalityInterviewAudio() {
        List<PersonalityInterview> interviews = personalityInterviewRepository.findAll();

        for (PersonalityInterview interview : interviews) {
            String audioUrl = googleTtsService.synthesizeAndUploadToPath(interview.getDescription(), "personality/questions/");

            personalityInterviewAudioRepository.findById(interview.getId())
                    .ifPresentOrElse(
                            audio -> audio.updateAudioUrl(audioUrl),
                            () -> personalityInterviewAudioRepository.save(
                                    new PersonalityInterviewAudio(interview, audioUrl)
                            )
                    );
        }

        return ResponseEntity.ok("총 " + interviews.size() + "건 TTS 생성 및 저장 완료");
    }

}
