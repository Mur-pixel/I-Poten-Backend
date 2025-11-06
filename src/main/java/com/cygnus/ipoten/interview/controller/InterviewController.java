package com.cygnus.ipoten.interview.controller;

import com.cygnus.ipoten.account.service.AccountService;
import com.cygnus.ipoten.authentication.service.AuthenticationService;
import com.cygnus.ipoten.infrastructure.external.email.EmailService;
import com.cygnus.ipoten.interview.controller.request_form.InterviewCreateRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.InterviewEndRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.InterviewProgressRequestForm;
import com.cygnus.ipoten.interview.controller.request_form.InterviewResultRequestForm;
import com.cygnus.ipoten.interview.controller.response_form.InterviewCreateResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.InterviewProgressResponseForm;
import com.cygnus.ipoten.interview.controller.response_form.InterviewResultListForm;
import com.cygnus.ipoten.interview.controller.response_form.InterviewResultResponseForm;
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

    @PostMapping("/create")
    public ResponseEntity<InterviewCreateResponseForm> interviewCreate(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody InterviewCreateRequestForm interviewCreateRequestForm) {

        log.info("면접 요청 !  첫번째 질문 옴: {}", interviewCreateRequestForm);

        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);

        InterviewCreateResponse interviewCreateResponse = interviewService.createInterview(interviewCreateRequestForm, accountId, userToken);


        return ResponseEntity.ok(InterviewCreateResponseForm.of(interviewCreateResponse));

    }


    @PostMapping("/progress")
    public ResponseEntity<InterviewProgressResponseForm> progressInterview(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody InterviewProgressRequestForm interviewProgressRequestForm
    ) {
        InterviewProgressResponse interviewProgressResponse = interviewService.execute(
                interviewProgressRequestForm.getInterviewType(), interviewProgressRequestForm, userToken);

        return ResponseEntity.ok(interviewProgressResponse.toInterviewProgressResponseForm());
    }

    @PostMapping("/end")
    public ResponseEntity<Void> endInterview(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody InterviewEndRequestForm interviewEndRequestForm
    ){
        interviewService.endInterview(interviewEndRequestForm, userToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestBody InterviewResultRequestForm interviewResultRequestForm
    ){
        InterviewResultResponse interviewResultResponse = interviewService.interviewResult(interviewResultRequestForm);

        emailService.sendInterviewResultNotification(interviewResultResponse.getSender(), interviewResultResponse.getResult().getInterview_id());


        return ResponseEntity.ok().build();
    }

    @GetMapping("/result/{interviewId}")
    public ResponseEntity<InterviewResultResponseForm> getInterviewResult(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long interviewId
    ){
        log.info("면접 결과 조회 요청 - interviewId: {}, userToken: {}", interviewId, userToken);
        
        // userToken 검증
        Long accountId = redisCacheService.getValueByKey(userToken, Long.class);
        if (accountId == null) {
            log.warn("유효하지 않은 userToken: {}", userToken);
            return ResponseEntity.status(401).build();
        }
        boolean checkInterviewOwnership = interviewResultService.checkInterviewOwnership(accountId, interviewId);

//        if (!checkInterviewOwnership) {
//            log.info("면접 결과 조회 권한이 없습니다");
//            return ResponseEntity.status(401).build();
//        }
        InterviewResultResponseForm interviewResult = interviewResultService.getInterviewResult(interviewId);

        return ResponseEntity.ok(interviewResult);
    }


    @GetMapping("/result/list")
    public ResponseEntity<InterviewResultListForm> getInterviewResult(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = authenticationService.getAccountIdByUserToken(userToken);

        List<InterviewResultListResponse> interviewResultListByAccountId = interviewService.getInterviewResultListByAccountId(accountId);

        return ResponseEntity.ok(new InterviewResultListForm(interviewResultListByAccountId));

    }




}
