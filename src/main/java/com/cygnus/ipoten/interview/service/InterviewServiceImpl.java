package com.cygnus.ipoten.interview.service;


import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.account_project.service.AccountProjectService;
import com.cygnus.ipoten.google_tts.service.GoogleTtsService;
import com.cygnus.ipoten.infrastructure.external.fastapi.client.FastApiEndInterview;
import com.cygnus.ipoten.interview.controller.request.InterviewAccountProjectRequest;
import com.cygnus.ipoten.interview.controller.request.InterviewEndRequest;
import com.cygnus.ipoten.interview.controller.request_form.*;
import com.cygnus.ipoten.interview.controller.response_form.NormalInterviewCreateResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.PersonalityInterviewResultResponseForm;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final Map<String, InterviewProcessStrategy> interviewProcessStrategies;
    private final Map<String, NormalInterviewProgressStrategy> normalInterviewProgressStrategies;
    private final AccountProjectService accountProjectService;
    private final FastApiEndInterview fastApiEndInterview;
    private final InterviewResultService interviewResultService;
    private final InterviewResultDetailService interviewResultDetailService;
    private final InterviewScoreService interviewScoreService;
    private final GoogleTtsService googleTtsService;

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
            Account account = accountService.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("인터뷰 생성에서 account를 찾지 못함"));

            IntervieweeProfile intervieweeProfile = intervieweeProfileService
                    .createIntervieweeProfile(interviewCreateRequestForm.toIntervieweeProfileRequest());

            Interview interview = new Interview(account, intervieweeProfile, interviewCreateRequestForm.getInterviewType(), InterviewPlan.PREMIUM);
            interview = interviewRepository.save(interview);
            log.info(" ✅ 인터뷰 확인 : {}", interview.getId());

            InterviewQA interviewQA = interviewQAService
                    .createInterviewQA(interviewCreateRequestForm.toInterviewQARequest(interview));

            List<InterviewAccountProjectRequest> interviewAccountProjectRequests =
                    interviewCreateRequestForm.getInterviewAccountProjectRequests();
            accountProjectService.saveAllByInterviewAccountProjectRequest(interviewAccountProjectRequests, account);
            log.info("✅ AccountProject 저장 완료, 요청 개수: {}",
                    interviewAccountProjectRequests != null ? interviewAccountProjectRequests.size() : 0);

            InterviewProgressRequestForm interviewProgressRequestForm = new InterviewProgressRequestForm(
                    interview.getId(),
                    1,
                    interviewCreateRequestForm.getInterviewType(),
                    interviewCreateRequestForm.getFirstAnswer(),
                    interviewQA.getId()
            );

            log.info("인터뷰 시퀀스 :  {}", interviewProgressRequestForm.getInterviewSequence());

            InterviewProgressResponse interviewProgressResponse = execute(
                    interviewCreateRequestForm.getInterviewType(),
                    interviewProgressRequestForm,
                    userToken
            );

            String questionTTS = googleTtsService.synthesizeAndUpload(interviewProgressResponse.getInterviewQuestionText());

            return interviewProgressResponse.toInterviewCreateResponseByTTS(questionTTS, interviewProgressResponse.getInterviewQuestionText());

        } catch (Exception e) {
            log.error("❌ createInterview 실행 중 예외 발생", e);
            throw e; // 그대로 예외를 던져 클라이언트에 500 반환
        }
    }

    @Override
    public NormalInterviewProgressResponse createNormalInterview(List<InterviewWithAudio> interviewList, NormalInterviewCreateRequestForm normalInterviewCreateRequestForm, Long accountId) {

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

        String typeKey = String.valueOf(type);
        InterviewProcessStrategy strategy = interviewProcessStrategies.get(typeKey);
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 인터뷰 타입: " + typeKey);
        }
        InterviewProgressResponse process = strategy.process(form, userToken);
        String questionTTS = googleTtsService.synthesizeAndUpload(process.getInterviewQuestionText());


        return process.updateInterviewQuestion(questionTTS, process.getInterviewQuestionText());
    }

    @Override
    public NormalInterviewCreateResponseForm execute(InterviewType type, NormalInterviewCreateRequestForm form, String userToken) {

        String typeKey = String.valueOf(type);
        NormalInterviewProgressStrategy strategy = normalInterviewProgressStrategies.get(typeKey);
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 인터뷰 타입: " + typeKey);
        }
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

    @Transactional
    @Override
    public void submitPersonalityInterviewAnswers(NormalInterviewSubmitRequestForm form, Long accountId) {
        Interview interview = interviewRepository.findById(form.getInterviewId())
                .orElseThrow(() -> new IllegalArgumentException("인터뷰를 찾을 수 없음"));

        // ✅ 소유권 확인: 요청한 accountId와 인터뷰의 주인이 같은지 확인
        if (!interview.getAccount().getId().equals(accountId)) {
            throw new SecurityException("인터뷰 제출 권한이 없습니다.");
        }

        // ✅ 유형 확인: 인성 면접(PERSONAL) 타입인지 확인
        if (interview.getInterviewType() != InterviewType.PERSONAL) {
            throw new IllegalArgumentException("인성 면접 답변만 제출 가능한 엔드포인트입니다.");
        }

        for (NormalInterviewSubmitRequestForm.QAItem qaItem : form.getQaList()) {
            interviewQAService.saveInterviewQAByInterview(
                    interview,
                    new InterviewQA(interview, qaItem.getQuestion(), qaItem.getAnswer())
            );
        }

        interview.setFinished(true);
        interviewRepository.save(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalityInterviewResultResponseForm getPersonalityInterviewResult(Long interviewId, Long accountId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException("인터뷰를 찾을 수 없음"));

        // ✅ 소유권 확인: 요청한 accountId와 인터뷰의 주인이 같은지 확인
        if (!interview.getAccount().getId().equals(accountId)) {
            throw new SecurityException("인터뷰 결과 조회 권한이 없습니다.");
        }

        // ✅ 유형 확인: 인성 면접(PERSONAL) 타입인지 확인
        if (interview.getInterviewType() != InterviewType.PERSONAL) {
            throw new IllegalArgumentException("인성 면접 결과만 조회 가능한 엔드포인트입니다.");
        }

        List<com.cygnus.ipoten.interviewQA.entity.InterviewQA> allQA = interviewQAService.findAllByInterviewId(interviewId);

        List<PersonalityInterviewResultResponseForm.QAItem> qaItems = allQA.stream()
                .map(qa -> PersonalityInterviewResultResponseForm.QAItem.builder()
                        .question(qa.getQuestion())
                        .answer(qa.getAnswer())
                        .build())
                .collect(Collectors.toList());

        return PersonalityInterviewResultResponseForm.builder()
                .interviewId(interviewId)
                .qaList(qaItems)
                .build();
    }


}
