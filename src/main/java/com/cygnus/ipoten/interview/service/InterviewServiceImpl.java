package com.cygnus.ipoten.interview.service;


import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.account_project.service.AccountProjectService;
import com.cygnus.ipoten.infrastructure.external.fastapi.client.FastApiEndInterview;
import com.cygnus.ipoten.interview.controller.request.InterviewAccountProjectRequest;
import com.cygnus.ipoten.interview.controller.request.InterviewEndRequest;
import com.cygnus.ipoten.interview.controller.request_form.*;
import com.cygnus.ipoten.interview.controller.response_form.NormalInterviewCreateResponseForm;
import com.cygnus.ipoten.interview.entity.Interview;
import com.cygnus.ipoten.interview.entity.InterviewPlan;
import com.cygnus.ipoten.interview.entity.InterviewType;
import com.cygnus.ipoten.interview.repository.InterviewRepository;
import com.cygnus.ipoten.interview.service.response.*;
import com.cygnus.ipoten.interview.service.strategy.interview_strategy.InterviewProcessStrategy;
import com.cygnus.ipoten.interview.service.strategy.normal_interview_strategy.NormalInterviewProgressStrategy;
import com.cygnus.ipoten.interviewQA.entity.InterviewQA;
import com.cygnus.ipoten.interviewQA.service.InterviewQAService;
import com.cygnus.ipoten.interview_result.entity.InterviewResult;
import com.cygnus.ipoten.interview_result.entity.InterviewResultDetail;
import com.cygnus.ipoten.interview_result.service.InterviewResultDetailService;
import com.cygnus.ipoten.interview_result.service.InterviewResultService;
import com.cygnus.ipoten.interview_score.entity.InterviewScore;
import com.cygnus.ipoten.interview_score.service.InterviewScoreService;
import com.cygnus.ipoten.interviewee_profile.entity.IntervieweeProfile;
import com.cygnus.ipoten.interviewee_profile.service.IntervieweeProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final AccountService accountService;
    private final IntervieweeProfileService intervieweeProfileService;
    private final InterviewQAService interviewQAService;
    private final InterviewRepository interviewRepository;
    private final ApplicationContext context;
    private final AccountProjectService accountProjectService;
    private final FastApiEndInterview fastApiEndInterview;
    private final InterviewResultService interviewResultService;
    private final InterviewResultDetailService interviewResultDetailService;
    private final InterviewScoreService interviewScoreService;

    @Value("${current_server.end_interview_url}")
    private String callbackUrl;


//    @Override
//    public InterviewCreateResponse createInterview(InterviewCreateRequestForm interviewCreateRequestForm, Long accountId, String userToken) {
//
//        Account account = accountService.findById(accountId)
//                .orElseThrow(() -> new IllegalArgumentException("인터뷰 생성에서 account를 찾지 못함"));
//        IntervieweeProfile intervieweeProfile = intervieweeProfileService.createIntervieweeProfile(interviewCreateRequestForm.toIntervieweeProfileRequest());
//        Interview interview = interviewRepository.save(new Interview(account, intervieweeProfile,  interviewCreateRequestForm.getInterviewType()));
//        InterviewQA interviewQA = interviewQAService.createInterviewQA(interviewCreateRequestForm.toInterviewQARequest(interview));
//        InterviewProgressRequestForm interviewProgressRequestForm = new InterviewProgressRequestForm(interview.getId(), 1, interviewCreateRequestForm.getInterviewType(), interviewCreateRequestForm.getFirstAnswer(), interviewQA.getId());
//
//        List<InterviewAccountProjectRequest> interviewAccountProjectRequests = interviewCreateRequestForm.getInterviewAccountProjectRequests();
//        accountProjectService.saveAllByInterviewAccountProjectRequest(interviewAccountProjectRequests, account);
//
//        InterviewProgressResponse interviewProgressResponse = execute(
//                interviewCreateRequestForm.getInterviewType(),
//                interviewProgressRequestForm,
//                userToken
//        );
//
//
//        return interviewProgressResponse.toInterviewCreateResponse();
//
//
//    }

    @Transactional
    @Override
    public InterviewCreateResponse createInterview(
            InterviewCreateRequestForm interviewCreateRequestForm,
            Long accountId,
            String userToken) {
        try {
            log.info("1️⃣ Account 조회 시작, accountId={}", accountId);
            Account account = accountService.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("인터뷰 생성에서 account를 찾지 못함"));
            log.info("✅ Account 조회 완료: {}", account.getId());

            log.info("2️⃣ IntervieweeProfile 생성 및 저장 시작");
            IntervieweeProfile intervieweeProfile = intervieweeProfileService
                    .createIntervieweeProfile(interviewCreateRequestForm.toIntervieweeProfileRequest());
            log.info("✅ IntervieweeProfile 생성 완료: {}", intervieweeProfile.getId());

            log.info("3️⃣ Interview 생성 및 저장 시작");
            Interview interview = new Interview(account, intervieweeProfile, interviewCreateRequestForm.getInterviewType(), InterviewPlan.PREMIUM);
            interview = interviewRepository.save(interview);
            log.info("✅ Interview 생성 완료: {}", interview.getId());

            log.info("4️⃣ InterviewQA 생성 시작");
            InterviewQA interviewQA = interviewQAService
                    .createInterviewQA(interviewCreateRequestForm.toInterviewQARequest(interview));
            log.info("✅ InterviewQA 생성 완료: {}", interviewQA.getId());

            log.info("5️⃣ AccountProject 저장 시작");
            List<InterviewAccountProjectRequest> interviewAccountProjectRequests =
                    interviewCreateRequestForm.getInterviewAccountProjectRequests();
            accountProjectService.saveAllByInterviewAccountProjectRequest(interviewAccountProjectRequests, account);
            log.info("✅ AccountProject 저장 완료, 요청 개수: {}",
                    interviewAccountProjectRequests != null ? interviewAccountProjectRequests.size() : 0);

            log.info("6️⃣ InterviewProgress 실행 시작");
            InterviewProgressRequestForm interviewProgressRequestForm = new InterviewProgressRequestForm(
                    interview.getId(),
                    1,
                    interviewCreateRequestForm.getInterviewType(),
                    interviewCreateRequestForm.getFirstAnswer(),
                    interviewQA.getId()
            );

            InterviewProgressResponse interviewProgressResponse = execute(
                    interviewCreateRequestForm.getInterviewType(),
                    interviewProgressRequestForm,
                    userToken
            );
            log.info("✅ InterviewProgress 실행 완료");

            log.info("🎉 InterviewCreateResponse 반환 준비");
            return interviewProgressResponse.toInterviewCreateResponse();

        } catch (Exception e) {
            log.error("❌ createInterview 실행 중 예외 발생", e);
            throw e; // 그대로 예외를 던져 클라이언트에 500 반환
        }
    }

    @Override
    public NormalInterviewProgressResponse createNormalInterview(List<String> interviewList, NormalInterviewCreateRequestForm normalInterviewCreateRequestForm, Long accountId) {

        log.info("1️⃣ Account 조회 시작, accountId={}", accountId);
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("인터뷰 생성에서 account를 찾지 못함"));
        log.info("✅ Account 조회 완료: {}", account.getId());

        log.info("3️⃣ Interview 생성 및 저장 시작");
        Interview interview = new Interview(account, null, normalInterviewCreateRequestForm.getInterviewType(), InterviewPlan.NORMAL);
        interview = interviewRepository.save(interview);
        log.info("✅ Interview 생성 완료: {}", interview.getId());


        return new NormalInterviewProgressResponse(interview.getId(), interviewList);
    }


    @Override
    public InterviewProgressResponse execute(InterviewType type, InterviewProgressRequestForm form, String userToken) {

        log.info("✅ 인터뷰 프로그레스 시도");
        log.info("✅ 인터뷰 내용 : {},  {},  {}, {}", form.getInterviewId(),form.getInterviewQAId(), form.getInterviewSequence(), form.getAnswer());

        InterviewProcessStrategy strategy = context.getBean(String.valueOf(type), InterviewProcessStrategy.class);

        return strategy.process(form, userToken);
    }

    @Override
    public NormalInterviewCreateResponseForm execute(InterviewType type, NormalInterviewCreateRequestForm form, String userToken) {

        NormalInterviewProgressStrategy strategy = context.getBean(String.valueOf(type), NormalInterviewProgressStrategy.class);
        NormalInterviewProgressResponse process = strategy.process(form, userToken);

        return process.toNormalInterviewCreateResponseForm();
    }


    @Transactional
    @Override
    public void endInterview(InterviewEndRequestForm interviewEndRequestForm, String userToken) {

        try {
            Interview interview = interviewRepository.findById(interviewEndRequestForm.getInterviewId())
                    .orElseThrow(() -> new IllegalArgumentException("인터뷰 종류 때 인터뷰를 찾을 수 없음"));
            interview.setSender(interviewEndRequestForm.getSender());
            interview.setFinished(true);
            interviewRepository.save(interview);


            InterviewEndRequest endInterviewRequestEndInterviewRequest = createEndInterviewRequestEndInterviewRequest(interviewEndRequestForm, userToken);

            fastApiEndInterview.endInterview(endInterviewRequestEndInterviewRequest);

        } catch (Exception e) {
            e.printStackTrace();
            log.info("인터뷰 종료 시 오류 발생");
        }



    }

    @Override
    public InterviewEndRequest createEndInterviewRequestEndInterviewRequest(InterviewEndRequestForm interviewEndRequestForm, String userToken) {
        Long interviewId = interviewEndRequestForm.getInterviewId();

        interviewQAService.saveInterviewAnswer(interviewEndRequestForm.getInterviewQAId(), interviewEndRequestForm.getAnswer());

        List<InterviewQA> allQA = interviewQAService.findAllByInterviewId(interviewEndRequestForm.getInterviewId());

        if (allQA.isEmpty()) {
            throw new IllegalArgumentException("인터뷰 종료 때 해당 인터뷰의 질문과 답변을 찾을 수 없습니다");
        }

        if (allQA.size() != 6) {
            throw new IllegalArgumentException("인터뷰 종료 때 인터뷰의 질문과 답변이 전부 존재 하지 않습니다");
        }

        List<String> questions = allQA.stream()
                .map(InterviewQA::getQuestion)
                .collect(Collectors.toList());

        List<String> answers = allQA.stream()
                .map(InterviewQA::getAnswer)
                .collect(Collectors.toList());


        return new InterviewEndRequest(
                userToken, interviewId, questions, answers, callbackUrl
        );
    }

    @Override
    public Optional<Interview> findById(Long id) {
        return interviewRepository.findById(id);
    }

    @Transactional
    @Override
    public InterviewResultResponse interviewResult(InterviewResultRequestForm interviewResultRequestForm) {

        Interview interview = findById(interviewResultRequestForm.getResult().getInterview_id())
                .orElseThrow(() -> new IllegalArgumentException("인터뷰 결과 생성 때 인터뷰를 찾을 수 없음"));


        InterviewResult interviewResult = interviewResultService.createInterviewResult(interviewResultRequestForm);

        List<InterviewResultDetail> interviewResultDetail = interviewResultDetailService.createInterviewResultDetail(interviewResultRequestForm, interviewResult.getId());

        InterviewScore interviewScore = interviewScoreService.createInterviewScore(interviewResultRequestForm);


        return new InterviewResultResponse(
                interviewResultRequestForm.getUserToken(),
                interviewResultRequestForm.getResult(),
                interviewResultRequestForm.getStatus(),
                interviewResultRequestForm.getError(),
                interview.getSender()
        );

    }

    @Override
    public List<InterviewResultListResponse> getInterviewResultListByAccountId(Long accountId) {

        List<Interview> interviewResultListByAccountId = interviewRepository.getInterviewResultListByAccountId(accountId);
        List<InterviewResultListResponse> interviewResultListResponses = new ArrayList<>();
        for (Interview interview : interviewResultListByAccountId) {
            InterviewResultListResponse interviewResultListResponse = new InterviewResultListResponse(
                    interview.isFinished(),
                    interview.getCreatedAt(),
                    interview.getSender(),
                    interview.getInterviewType(),
                    interview.getId()
            );
            interviewResultListResponses.add(interviewResultListResponse);
        }

        return interviewResultListResponses;
    }


}
