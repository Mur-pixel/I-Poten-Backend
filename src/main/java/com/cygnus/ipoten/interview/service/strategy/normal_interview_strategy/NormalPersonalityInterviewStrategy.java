package com.cygnus.ipoten.interview.service.strategy.normal_interview_strategy;


import com.cygnus.ipoten.interview.controller.request_form.NormalInterviewCreateRequestForm;
import com.cygnus.ipoten.interview.service.InterviewService;
import com.cygnus.ipoten.interview.service.response.InterviewWithAudio;
import com.cygnus.ipoten.interview.service.response.NormalInterviewProgressResponse;
import com.cygnus.ipoten.personality_interview.entity.PersonalityInterview;
import com.cygnus.ipoten.personality_interview.repository.PersonalityInterviewAudioRepository;
import com.cygnus.ipoten.personality_interview.service.PersonalityInterviewService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;


@Component("PERSONAL")
public class NormalPersonalityInterviewStrategy implements NormalInterviewProgressStrategy {

    private final InterviewService interviewService;
    private final PersonalityInterviewService personalityInterviewService;
    private final PersonalityInterviewAudioRepository personalityInterviewAudioRepository;
    private final RedisCacheService redisCacheService;

    public NormalPersonalityInterviewStrategy(
            @Lazy InterviewService interviewService,
            PersonalityInterviewService personalityInterviewService,
            PersonalityInterviewAudioRepository personalityInterviewAudioRepository,
            RedisCacheService redisCacheService) {
        this.interviewService = interviewService;
        this.personalityInterviewService = personalityInterviewService;
        this.personalityInterviewAudioRepository = personalityInterviewAudioRepository;
        this.redisCacheService = redisCacheService;
    }

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
