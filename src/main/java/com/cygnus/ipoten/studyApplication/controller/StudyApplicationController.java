package com.cygnus.ipoten.studyApplication.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.studyApplication.controller.request_form.CreateStudyApplicationRequestForm;
import com.cygnus.ipoten.studyApplication.controller.response_form.ApplicationForHostResponseForm;
import com.cygnus.ipoten.studyApplication.controller.response_form.CreateStudyApplicationResponseForm;
import com.cygnus.ipoten.studyApplication.controller.response_form.ListMyApplicationResponseForm;
import com.cygnus.ipoten.studyApplication.service.StudyApplicationService;
import com.cygnus.ipoten.studyApplication.service.request.ProcessApplicationRequest;
import com.cygnus.ipoten.studyApplication.service.response.ApplicationForHostResponse;
import com.cygnus.ipoten.studyApplication.service.response.CreateStudyApplicationResponse;
import com.cygnus.ipoten.studyApplication.service.response.ListMyApplicationResponse;
import com.cygnus.ipoten.studyApplication.service.response.MyApplicationStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study-applications")
public class StudyApplicationController {

    private final StudyApplicationService studyApplicationService;
    private final RedisCacheService redisCacheService;

    @PostMapping
    public ResponseEntity<CreateStudyApplicationResponseForm> applyToStudy(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody CreateStudyApplicationRequestForm requestForm
    ) {

        // Redis 조회 및 null 체크
        Long applicantId = redisCacheService.getValueByKey(userToken, Long.class);
        if (applicantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CreateStudyApplicationResponse serviceResponse =
                studyApplicationService.applyToStudy(requestForm.toServiceRequest(applicantId));

        CreateStudyApplicationResponseForm responseForm = CreateStudyApplicationResponseForm.from(serviceResponse);

        log.info("스터디 지원 성공. applicationId={}", responseForm.getApplicationId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateStudyApplicationResponseForm
                        .from(serviceResponse));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ListMyApplicationResponseForm>> getMyApplications(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long applicantId = redisCacheService.getValueByKey(userToken, Long.class);

        List<ListMyApplicationResponse> serviceResponse = studyApplicationService.findMyApplications(applicantId);

        // 서비스 DTO를 컨트롤러 Form으로 변환 (프론트엔드와 최종 약속)
        List<ListMyApplicationResponseForm> response = serviceResponse.stream()
                .map(ListMyApplicationResponseForm::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-status/{studyRoomId}")
    public ResponseEntity<MyApplicationStatusResponse> getMyApplicationStatus(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken) {

        // 토큰이 없으면 applicantId는 null
        Long applicantId = (userToken != null) ? redisCacheService.getValueByKey(userToken, Long.class) : null;

        MyApplicationStatusResponse response = studyApplicationService.findMyApplicationStatus(studyRoomId, applicantId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{applicationId}")
    public ResponseEntity<Void> cancelApplication(
            @PathVariable Long applicationId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long applicantId = redisCacheService.getValueByKey(userToken, Long.class);

        studyApplicationService.cancelApplication(applicationId, applicantId);

        return ResponseEntity.noContent().build();
    }

    // 스터디모임의 호스트가 해당 스터디모임의 참가신청 목록을 조회하는 API
    @GetMapping("/host/{studyRoomId}")
    public ResponseEntity<List<ApplicationForHostResponseForm>> getApplicationForHost(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken) {
        Long hostId = redisCacheService.getValueByKey(userToken, Long.class);

        List<ApplicationForHostResponse> serviceResponse = studyApplicationService.findApplicationsForHost(studyRoomId, hostId);
        List<ApplicationForHostResponseForm> response = serviceResponse.stream()
                .map(ApplicationForHostResponseForm::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // 호스트가 받은 참가신청에 대한 처리(수락/거절)하는 API
    @PatchMapping("/{applicationId}/process")
    public ResponseEntity<Void> processApplication(
            @PathVariable Long applicationId,
            @CookieValue(value = "userToken", required = false) String userToken,
            @RequestBody ProcessApplicationRequest request) {

        Long hostId = redisCacheService.getValueByKey(userToken, Long.class);

        studyApplicationService.processApplication(applicationId, hostId, request);

        return ResponseEntity.ok().build();
    }

    // 이 컨트롤러 내에서 발생하는 IllegalStateException을 특별 처리하는 핸들러를 추가
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
}