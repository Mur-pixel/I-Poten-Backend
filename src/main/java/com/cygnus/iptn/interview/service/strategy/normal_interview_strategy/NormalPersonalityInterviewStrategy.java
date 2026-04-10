package com.cygnus.iptn.interview.service.strategy.normal_interview_strategy;


import com.cygnus.iptn.interview.controller.request_form.NormalInterviewCreateRequestForm;
import com.cygnus.iptn.interview.service.InterviewService;
import com.cygnus.iptn.interview.service.response.InterviewWithAudio;
import com.cygnus.iptn.interview.service.response.NormalInterviewProgressResponse;
import com.cygnus.iptn.personality_interview.entity.PersonalityInterview;
import com.cygnus.iptn.personality_interview.repository.PersonalityInterviewAudioRepository;
import com.cygnus.iptn.personality_interview.service.PersonalityInterviewService;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@RequiredArgsConstructor
@Component("PERSONAL")
public class NormalPersonalityInterviewStrategy implements NormalInterviewProgressStrategy {

    private final InterviewService interviewService;
    private final PersonalityInterviewService personalityInterviewService;
    private final PersonalityInterviewAudioRepository personalityInterviewAudioRepository;
    private final RedisCacheService redisCacheService;

    @Override
    public NormalInterviewProgressResponse process(NormalInterviewCreateRequestForm interviewProgressRequestForm, String userToken) {

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        List<PersonalityInterview> personalityInterviews = personalityInterviewService.getPersonalityInterviews();

        List<InterviewWithAudio> mapped = personalityInterviews.stream()
                .map(interview -> new InterviewWithAudio(
                        interview.getDescription(),
                        personalityInterviewAudioRepository.findById(interview.getId())
                                .map(audio -> audio.getAudioUrl())
                                .orElse(null)
                ))
                .toList();

        return interviewService.createNormalInterview(mapped, interviewProgressRequestForm, accountId);

    }

}
