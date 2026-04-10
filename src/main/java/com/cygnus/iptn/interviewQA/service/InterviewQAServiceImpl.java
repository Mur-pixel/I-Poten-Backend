package com.cygnus.iptn.interviewQA.service;

import com.cygnus.iptn.interview.controller.request.InterviewQARequest;
import com.cygnus.iptn.interview.entity.Interview;
import com.cygnus.iptn.interviewQA.entity.InterviewQA;
import com.cygnus.iptn.interviewQA.repository.InterviewQARepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InterviewQAServiceImpl implements InterviewQAService {

    private final InterviewQARepository interviewQARepository;

    @Override
    public void saveInterviewQAByInterview(Interview interview, InterviewQA interviewQA) {
        interviewQA.setInterview(interview);
        interviewQARepository.save(interviewQA);
    }

    @Override
    public InterviewQA createInterviewQA(InterviewQARequest interviewQARequest) {

        return interviewQARepository.save(
                new InterviewQA(
                        interviewQARequest.getInterview(),
                        interviewQARequest.getFirstQuestion(),
                        interviewQARequest.getFirstAnswer()
                )
        );
    }

    @Override
    public InterviewQA createInterviewQuestion(Interview interview, String interviewQuestion) {
        return interviewQARepository.save(
                new InterviewQA(
                        interview,
                        interviewQuestion
                )
        );
    }

    @Override
    public InterviewQA createInterviewQaByInterview(Interview interview) {
        return interviewQARepository.save(new InterviewQA(interview));
    }

    @Override
    public InterviewQA saveInterviewAnswer(Long interviewQAId, String interviewAnswer) {
        InterviewQA interviewQA = findById(interviewQAId)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 InterviewQA를 찾을 수 없습니다"));

        interviewQA.setAnswer(interviewAnswer);

        return interviewQARepository.save(interviewQA);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<InterviewQA> findById(Long interviewQAId) {
        return interviewQARepository.findById(interviewQAId);
    }

    @Override
    public List<InterviewQA> findAllByInterviewId(Long interviewId) {
        return interviewQARepository.findByInterview_Id(interviewId);
    }


}
