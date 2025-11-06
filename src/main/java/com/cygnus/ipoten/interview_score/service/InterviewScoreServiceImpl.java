package com.cygnus.ipoten.interview_score.service;

import com.cygnus.ipoten.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.service.InterviewService;
import com.cygnus.ipoten.interview_score.entity.InterviewScore;
import com.cygnus.ipoten.interview_score.repository.InterviewScoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class InterviewScoreServiceImpl implements InterviewScoreService {

    private final InterviewScoreRepository interviewScoreRepository;

    // 순환 참조 해결을 위해 @Lazy 사용
    private final InterviewService interviewService;

    public InterviewScoreServiceImpl(
            InterviewScoreRepository interviewScoreRepository,
            @Lazy InterviewService interviewService
    ) {
        this.interviewScoreRepository = interviewScoreRepository;
        this.interviewService = interviewService;
    }

    @Override
    public InterviewScore createInterviewScore(InterviewResultRequestForm interviewResultRequestForm) {

        Long interviewId = interviewResultRequestForm.getResult().getInterview_id();
        Interview interview = interviewService.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("인터뷰 점수 저장 때 인터뷰를 찾을 수 없습니다"));

        InterviewResultRequestForm.EvaluationResult evaluationResult = interviewResultRequestForm.getResult().getEvaluation_result();

        InterviewScore interviewScore = new InterviewScore(
                interview,
                evaluationResult.getCommunication(),
                evaluationResult.getProductivity(),
                evaluationResult.getDocumentation_skills(),
                evaluationResult.getFlexibility(),
                evaluationResult.getProblem_solving(),
                evaluationResult.getTechnical_skills()
        );

        return interviewScoreRepository.save(interviewScore);
    }

    @Override
    public InterviewScore findByInterviewId(Long interviewId) {
        return interviewScoreRepository.findByInterviewId(interviewId);
    }
}
