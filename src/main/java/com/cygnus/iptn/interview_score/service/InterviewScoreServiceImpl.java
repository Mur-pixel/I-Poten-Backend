package com.cygnus.iptn.interview_score.service;

import com.cygnus.iptn.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.iptn.interview.entity.Interview;
import com.cygnus.iptn.interview.service.InterviewService;
import com.cygnus.iptn.interview_score.entity.InterviewScore;
import com.cygnus.iptn.interview_score.repository.InterviewScoreRepository;
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

        if (evaluationResult == null) {
            log.warn("⚠️ evaluation_result가 null입니다. interviewId: {} — 기본값 0으로 저장합니다.", interviewId);
            evaluationResult = new InterviewResultRequestForm.EvaluationResult();
        }

        InterviewScore interviewScore = new InterviewScore(
                interview,
                evaluationResult.getCommunication()          != null ? evaluationResult.getCommunication()          : 0,
                evaluationResult.getProductivity()           != null ? evaluationResult.getProductivity()           : 0,
                evaluationResult.getDocumentation_skills()   != null ? evaluationResult.getDocumentation_skills()   : 0,
                evaluationResult.getFlexibility()            != null ? evaluationResult.getFlexibility()            : 0,
                evaluationResult.getProblem_solving()        != null ? evaluationResult.getProblem_solving()        : 0,
                evaluationResult.getTechnical_skills()       != null ? evaluationResult.getTechnical_skills()       : 0
        );

        return interviewScoreRepository.save(interviewScore);
    }

    @Override
    public InterviewScore findByInterviewId(Long interviewId) {
        return interviewScoreRepository.findByInterviewId(interviewId);
    }
}
