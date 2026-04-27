package com.cygnus.ipoten.interview.service.strategy.interview_strategy;


import com.cygnus.ipoten.interview.controller.request_form.InterviewProgressRequestForm;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interview.service.strategy.sequence_strategy.InterviewSequenceStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component("COMPANY")
public class CompanyInterviewStrategy implements InterviewProcessStrategy {

    private final Map<String, InterviewSequenceStrategy> sequenceStrategies;

    @Override
    public InterviewProgressResponse process(
            InterviewProgressRequestForm interviewProgressRequestForm, String userToken) {

        String sequenceKey = String.valueOf(interviewProgressRequestForm.getInterviewSequence());
        InterviewSequenceStrategy strategy = sequenceStrategies.get(sequenceKey);
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 인터뷰 시퀀스: " + sequenceKey);
        }

        return strategy.getQuestionByCompany(interviewProgressRequestForm.toInterviewSequenceRequest(), userToken);

    }
}
