package com.cygnus.ipoten.studyroom.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.studyApplication.service.StudyApplicationService;
import com.cygnus.ipoten.studyApplication.service.response.MyApplicationStatusResponse;
import com.cygnus.ipoten.studyroom.controller.request_Form.*;
import com.cygnus.ipoten.studyroom.controller.response_form.*;
import com.cygnus.ipoten.studyroom.service.StudyRoomService;
import com.cygnus.ipoten.studyroom.service.request.ListStudyRoomRequest;
import com.cygnus.ipoten.studyroom.service.request.TransferLeadershipRequest;
import com.cygnus.ipoten.studyroom.service.request.UpdateInterviewChannelRequest;
import com.cygnus.ipoten.studyroom.service.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/study-rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomService studyRoomService;
    private final RedisCacheService redisCacheService;
    private final StudyApplicationService studyApplicationService;

    @PostMapping
    public ResponseEntity<CreateStudyRoomResponseForm> createStudyRoom(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody CreateStudyRoomRequestForm requestForm) {

        Long hostId = redisCacheService.getValueByKey(userToken, Long.class);

        CreateStudyRoomResponse serviceResponse = studyRoomService.createStudyRoom(requestForm.toServiceRequest(hostId));
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateStudyRoomResponseForm.from(serviceResponse));
    }

    @GetMapping
    public ResponseEntity<ListStudyRoomResponseForm> getAllStudyRooms(
            @RequestParam(required = false) Long lastStudyId,
            @RequestParam int size) {
        ListStudyRoomRequest serviceRequest = new ListStudyRoomRequest(lastStudyId, size);
        ListStudyRoomResponse serviceResponse = studyRoomService.findAllStudyRooms(serviceRequest);
        return ResponseEntity.ok(ListStudyRoomResponseForm.from(serviceResponse));
    }

    @GetMapping("/{studyRoomId}")
    public ResponseEntity<ReadStudyRoomResponseForm> readStudyRoom(
            @PathVariable Long studyRoomId,
            // ✅ 쿠키에서 토큰을 가져옵니다.
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long currentUserId = null;
        if (userToken != null) {
            // ✅ Redis에서 사용자 ID를 조회합니다.
            currentUserId = redisCacheService.getValueByKey(userToken, Long.class);
        }

        // ✅ 조회한 사용자 ID를 서비스에 전달합니다.
        ReadStudyRoomResponse serviceResponse = studyRoomService.readStudyRoom(studyRoomId, currentUserId);
        return ResponseEntity.ok(ReadStudyRoomResponseForm.from(serviceResponse));
    }

    @GetMapping("/my-studies")
    public ResponseEntity<List<MyStudyResponse>> getMyStudies(
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        List<MyStudyResponse> myStudiesResponse = studyRoomService.findMyStudies(currentUserId);

        return ResponseEntity.ok(myStudiesResponse);
    }

    @GetMapping("/{studyRoomId}/members")
    public ResponseEntity<List<StudyMemberResponseForm>> getStudyMembers(
            @PathVariable Long studyRoomId) {
        List<StudyMemberResponse> serviceResponse = studyRoomService.getStudyMembers(studyRoomId);
        List<StudyMemberResponseForm> response = serviceResponse.stream()
                .map(StudyMemberResponseForm::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{studyRoomId}/role")
    public ResponseEntity<String> getUserRoleInStudyRoom(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        String role = studyRoomService.findUserRoleInStudyRoom(studyRoomId, currentUserId);
        return ResponseEntity.ok(role);
    }

    @PutMapping("/{studyRoomId}")
    public ResponseEntity<UpdateStudyRoomResponseForm> updateStudyRoom(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody UpdateStudyRoomRequestForm requestForm) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        UpdateStudyRoomResponse serviceResponse = studyRoomService.updateStudyRoom(
                studyRoomId, currentUserId, requestForm.toServiceRequest()
        );
        return ResponseEntity.ok(UpdateStudyRoomResponseForm.from(serviceResponse));
    }

    @PatchMapping("/{studyRoomId}/status")
    public ResponseEntity<Void> updateStudyRoomStatus(
            @PathVariable Long studyRoomId,
            @CookieValue(value = "userToken", required = false) String userToken,
            @RequestBody UpdateStudyRoomStatusRequestForm requestForm) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        studyRoomService.updateStudyRoomStatus(studyRoomId, currentUserId, requestForm.toServiceRequest());
        return ResponseEntity.ok().build();
    }


    @DeleteMapping("/{studyRoomId}")
    public ResponseEntity<Void> deleteStudyRoom(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        studyRoomService.deleteStudyRoom(studyRoomId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // 스터디에서 참가자가 탈퇴하는 API
    @DeleteMapping("/{studyRoomId}/membership")
    public ResponseEntity<Void> leaveStudyRoom(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken) {
        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        studyRoomService.leaveStudyRoom(studyRoomId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // 스터디에서 멤버를 강퇴시키는 API
    @DeleteMapping("/{studyRoomId}/members/{memberId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable Long studyRoomId,
            @PathVariable Long memberId,
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long leaderId = redisCacheService.getValueByKey(userToken, Long.class);

        studyRoomService.kickMember(studyRoomId, memberId, leaderId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{studyRoomId}/my-application")
    public ResponseEntity<MyApplicationStatusResponse> getMyApplicationStatus(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long applicantId = null;
        if (userToken != null) {
            applicantId = redisCacheService.getValueByKey(userToken, Long.class);
        }
        MyApplicationStatusResponse response = studyApplicationService.findMyApplicationStatus(studyRoomId, applicantId);
        return ResponseEntity.ok(response);
    }

    // 모의면접 채널 조회 API
    @GetMapping("/{studyRoomId}/interview-channels")
    public ResponseEntity<List<InterviewChannelResponse>> getInterviewChannels(
            @PathVariable Long studyRoomId) {
        List<InterviewChannelResponse> channels = studyRoomService.findInterviewChannels(studyRoomId);
        return ResponseEntity.ok(channels);
    }

    // 모의면접 채널 수정 API
    @PutMapping("/{studyRoomId}/interview-channels")
    public ResponseEntity<Void> updateInterviewChannel(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken") String userToken,
            @RequestBody UpdateInterviewChannelRequest request) {

        Long leaderId = redisCacheService.getValueByKey(userToken, Long.class);
        studyRoomService.updateInterviewChannel(studyRoomId, leaderId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{studyRoomId}/my-status")
    public ResponseEntity<MyApplicationStatusResponse> getMyStudyStatus(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long currentUserId = (userToken != null) ? redisCacheService.getValueByKey(userToken, Long.class) : null;

        MyApplicationStatusResponse response = studyRoomService.findMyStudyStatus(studyRoomId, currentUserId);
        return ResponseEntity.ok(response);
    }

    // 스터디모임 리더 위임 API
    @PatchMapping("/{studyRoomId}/transfer-leadership")
    public ResponseEntity<Void> transferLeadership(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody TransferLeadershipRequest request) {

        Long currentLeaderId = redisCacheService.getValueByKey(userToken, Long.class);

        studyRoomService.transferLeadership(studyRoomId, currentLeaderId, request.getNewLeaderId());

        return ResponseEntity.ok().build();
    }

}