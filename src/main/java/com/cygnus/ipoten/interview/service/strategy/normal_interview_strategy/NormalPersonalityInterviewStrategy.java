package com.cygnus.ipoten.interview.service.strategy.normal_interview_strategy;


import com.cygnus.ipoten.google_tts.service.GoogleTtsService;
import com.cygnus.ipoten.interview.controller.request_form.InterviewProgressRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.NormalInterviewCreateRequestForm;
import com.cygnus.ipoten.interview.service.InterviewService;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interview.service.response.InterviewWithAudio;
import com.cygnus.ipoten.interview.service.response.NormalInterviewProgressResponse;
import com.cygnus.ipoten.personality_interview.entity.PersonalityInterview;
import com.cygnus.ipoten.personality_interview.service.PersonalityInterviewService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@RequiredArgsConstructor
@Component("PERSONAL")
public class NormalPersonalityInterviewStrategy implements NormalInterviewProgressStrategy {

    private final InterviewService interviewService;
    private final PersonalityInterviewService personalityInterviewService;
    private final GoogleTtsService googleTtsService;
    private final RedisCacheService redisCacheService;

    @Override
    public NormalInterviewProgressResponse process(NormalInterviewCreateRequestForm interviewProgressRequestForm, String userToken) {

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        List<PersonalityInterview> personalityInterviews = personalityInterviewService.getPersonalityInterviews();


        List<String> interviewList = personalityInterviews.stream()
                .map(PersonalityInterview::getDescription)
                .toList();

        List<String> interviewQuestions = googleTtsService.synthesizeAndUploadList(interviewList);

        List<InterviewWithAudio> mapped = IntStream.range(0, personalityInterviews.size())
                .mapToObj(i -> new InterviewWithAudio(
                        personalityInterviews.get(i).getDescription(),
                        interviewQuestions.get(i)                     
                ))
                .toList();



        return interviewService.createNormalInterview(mapped, interviewProgressRequestForm, accountId);

    }

}
