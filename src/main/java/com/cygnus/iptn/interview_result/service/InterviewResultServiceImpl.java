package com.cygnus.iptn.interview_result.service;

import com.cygnus.iptn.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.iptn.interview.controller.response_form.InterviewResultResponseForm;
import com.cygnus.iptn.interview.entity.Interview;
import com.cygnus.iptn.interview.service.InterviewService;
import com.cygnus.iptn.interview_result.entity.InterviewResult;
import com.cygnus.iptn.interview_result.entity.InterviewResultDetail;
import com.cygnus.iptn.interview_result.repository.InterviewResultRepository;
import com.cygnus.iptn.interview_score.entity.InterviewScore;
import com.cygnus.iptn.interview_score.service.InterviewScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
public class InterviewResultServiceImpl implements InterviewResultService {

    private final InterviewResultRepository interviewResultRepository;
    private final InterviewService interviewService;
    private final InterviewResultDetailService interviewResultDetailService;
    private final InterviewScoreService interviewScoreService;

    public InterviewResultServiceImpl(
            InterviewResultRepository interviewResultRepository,
            @Lazy InterviewService interviewService, InterviewResultDetailService interviewResultDetailService, InterviewScoreService interviewScoreService
    ) {
        this.interviewResultRepository = interviewResultRepository;
        this.interviewService = interviewService;
        this.interviewResultDetailService = interviewResultDetailService;
        this.interviewScoreService = interviewScoreService;
    }


    @Override
    public InterviewResult createInterviewResult(InterviewResultRequestForm interviewResultRequestForm) {

        String overallComment = interviewResultRequestForm.getResult().getOverall_comment();
        Long interviewId = interviewResultRequestForm.getResult().getInterview_id();
        Interview interview = interviewService.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("인터뷰 결과 생성때 인터뷰를 찾을 수 없습니다."));


        return interviewResultRepository.save(
                new InterviewResult(interview, overallComment)
        );

    }

    @Override
    public InterviewResultResponseForm getInterviewResult(Long interviewId) {

        InterviewResult interviewResult = interviewResultRepository.findByInterviewId(interviewId);
        log.info("인터뷰 리졸트 아이디 : {}", interviewResult.getId());


        List<InterviewResultDetail> allByInterviewResultId = interviewResultDetailService.findAllByInterviewResultId(interviewResult.getId());
        InterviewScore interviewScore = interviewScoreService.findByInterviewId(interviewId);
        List<InterviewResultResponseForm.Qa> qas = convertInterviewResultDetailToResponseFormList(allByInterviewResultId);
        InterviewResultResponseForm.HexagonScore hexagonScore = new InterviewResultResponseForm.HexagonScore(
                interviewScore.getProductivity(),
                interviewScore.getCommunication(),
                interviewScore.getTechnicalSkills(),
                interviewScore.getDocumentationSkills(),
                interviewScore.getFlexibility(),
                interviewScore.getProblemSolving()
        );

        log.info("");
        log.info("인터뷰 내용  ");
        log.info("");

        for (InterviewResultResponseForm.Qa qa : qas) {
            log.info("{}", qa.getQuestion());
            log.info("{}", qa.getAnswer());
        }



        return new InterviewResultResponseForm(
                qas,
                hexagonScore,
                interviewResult.getOverallComment()
        );
    }

    @Override
    public boolean checkInterviewOwnership(Long accountId, Long interviewId) {
        log.info("권한 검증 시작함");
        InterviewResult interviewResult = interviewResultRepository.findByInterviewId(interviewId);

        if (interviewResult == null) {
            log.error("❌ interviewResult가 null입니다. interviewId: {}", interviewId);
            return false;
        }

        Interview interview = interviewResult.getInterview();
        if (interview == null) {
            log.error("❌ interviewResult는 있으나 interview가 null입니다. interviewResult.id: {}", interviewResult.getId());
            return false;
        }


        if (interview.getAccount().getId().equals(accountId)) {
            return true;
        } else{
            return false;
        }
    }

    @Override
    public List<InterviewResultResponseForm.Qa> convertInterviewResultDetailToResponseFormList(List<InterviewResultDetail> interviewResultDetail) {

        List<InterviewResultResponseForm.Qa> qa = new ArrayList<>();

        for (InterviewResultDetail resultDetail : interviewResultDetail) {
            InterviewResultResponseForm.Qa qaN = new InterviewResultResponseForm.Qa(
                    resultDetail.getQuestion(),
                    resultDetail.getAnswer(),
                    resultDetail.getIntent(),
                    resultDetail.getFeedback(),
                    resultDetail.getCorrection()
            );
            qa.add(qaN);
        }

        return qa;
    }
}
