package com.cygnus.ipoten.interview.controller;

import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.common.annotation.LoginToken;
import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.common.annotation.PublicEndpoint;
import com.cygnus.ipoten.fcm.service.FcmNotificationService;
import com.cygnus.ipoten.infrastructure.external.email.EmailService;
import com.cygnus.ipoten.interview.controller.request_form.*;
import com.cygnus.ipoten.interview.controller.response_form.*;
import com.cygnus.ipoten.interview.controller.response_form.PersonalityInterviewResultResponseForm;
import com.cygnus.ipoten.interview.service.InterviewService;
import com.cygnus.ipoten.interview.service.response.InterviewCreateResponse;
import com.cygnus.ipoten.interview.service.response.InterviewProgressResponse;
import com.cygnus.ipoten.interview.service.response.InterviewResultListResponse;
import com.cygnus.ipoten.interview.service.response.InterviewResultResponse;
import com.cygnus.ipoten.interview_result.service.InterviewResultService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final RedisCacheService redisCacheService;
    private final InterviewService interviewService;
    private final EmailService emailService;
    private final InterviewResultService interviewResultService;
    private final AccountService accountService;
    private final AuthenticationService authenticationService;
    private final FcmNotificationService fcmNotificationService;


    @PostMapping("/create")
    public ResponseEntity<InterviewCreateResponseForm> interviewCreate(
            @LoginUser Long accountId,
            @LoginToken String userToken,
            @RequestBody InterviewCreateRequestForm interviewCreateRequestForm) {

        log.info("면접 요청 !  첫번째 질문 옴: {}", interviewCreateRequestForm);
        InterviewCreateResponse interviewCreateResponse = interviewService.createInterview(interviewCreateRequestForm, accountId, userToken);
        return ResponseEntity.ok(InterviewCreateResponseForm.of(interviewCreateResponse));
    }

    @PostMapping("/create/normal")
    public ResponseEntity<NormalInterviewCreateResponseForm> interviewCreateNormal(
            @LoginToken String userToken,
            @RequestBody NormalInterviewCreateRequestForm normalInterviewCreateRequestForm) {

        log.info("노말 면접 시도 옴");
        NormalInterviewCreateResponseForm normalInterviewCreateResponseForm = interviewService.execute(
                normalInterviewCreateRequestForm.getInterviewType(), normalInterviewCreateRequestForm, userToken);
        return ResponseEntity.ok(normalInterviewCreateResponseForm);
    }

    @PostMapping("/progress")
    public ResponseEntity<InterviewProgressResponseForm> progressInterview(
            @LoginToken String userToken,
            @RequestBody InterviewProgressRequestForm interviewProgressRequestForm) {

        InterviewProgressResponse interviewProgressResponse = interviewService.execute(
                interviewProgressRequestForm.getInterviewType(), interviewProgressRequestForm, userToken);
        return ResponseEntity.ok(interviewProgressResponse.toInterviewProgressResponseForm());
    }

    @PostMapping("/end")
    public ResponseEntity<Void> endInterview(
            @LoginToken String userToken,
            @RequestBody InterviewEndRequestForm interviewEndRequestForm) {

        interviewService.endInterview(interviewEndRequestForm, userToken);
        return ResponseEntity.ok().build();
    }

    // FastAPI 콜백 — 인증 없이 호출됨
    @PublicEndpoint
    @PostMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestBody InterviewResultRequestForm interviewResultRequestForm) {

        InterviewResultResponse interviewResultResponse = interviewService.interviewResult(interviewResultRequestForm);

        try {
            emailService.sendInterviewResultNotification(interviewResultResponse.getSender(), interviewResultResponse.getResult().getInterview_id());
        } catch (Exception e) {
            log.error("이메일 발송 실패 - {}", e.getMessage());
        }

        Long accountId = redisCacheService.getValueByKey(interviewResultResponse.getUserToken(), Long.class);
        if (accountId != null) {
            fcmNotificationService.sendToAccount(accountId, "면접 결과가 도착했어요", "AI 면접 분석이 완료되었습니다. 결과를 확인해보세요!", interviewResultResponse.getResult().getInterview_id());
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/result/{interviewId}")
    public ResponseEntity<InterviewResultResponseForm> getInterviewResult(
            @LoginUser Long accountId,
            @PathVariable Long interviewId) {

        log.info("면접 결과 조회 요청 - interviewId: {}", interviewId);
        interviewResultService.checkInterviewOwnership(accountId, interviewId);
        InterviewResultResponseForm interviewResult = interviewResultService.getInterviewResult(interviewId);
        return ResponseEntity.ok(interviewResult);
    }

    @GetMapping("/result/list")
    public ResponseEntity<InterviewResultListForm> getInterviewResultList(@LoginUser Long accountId) {
        log.info("accountId: {}", accountId);
        List<InterviewResultListResponse> interviewResultListByAccountId = interviewService.getInterviewResultListByAccountId(accountId);
        return ResponseEntity.ok(new InterviewResultListForm(interviewResultListByAccountId));
    }

    @PostMapping("/normal/submit")
    public ResponseEntity<Void> submitPersonalityInterview(
            @LoginUser Long accountId,
            @RequestBody NormalInterviewSubmitRequestForm form) {

        try {
            interviewService.submitPersonalityInterviewAnswers(form, accountId);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/normal/result/{interviewId}")
    public ResponseEntity<PersonalityInterviewResultResponseForm> getPersonalityInterviewResult(
            @LoginUser Long accountId,
            @PathVariable Long interviewId) {

        try {
            PersonalityInterviewResultResponseForm result = interviewService.getPersonalityInterviewResult(interviewId, accountId);
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
