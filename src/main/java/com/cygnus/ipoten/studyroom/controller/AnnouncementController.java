package com.cygnus.ipoten.studyroom.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.studyroom.controller.request_Form.CreateAnnouncementRequestForm;
import com.cygnus.ipoten.studyroom.controller.request_Form.UpdateAnnouncementRequestForm;
import com.cygnus.ipoten.studyroom.controller.response_form.CreateAnnouncementResponseForm;
import com.cygnus.ipoten.studyroom.controller.response_form.ListAnnouncementResponseForm;
import com.cygnus.ipoten.studyroom.controller.response_form.ReadAnnouncementResponseForm;
import com.cygnus.ipoten.studyroom.service.AnnouncementService;
import com.cygnus.ipoten.studyroom.service.StudyRoomService;
import com.cygnus.ipoten.studyroom.service.response.CreateAnnouncementResponse;
import com.cygnus.ipoten.studyroom.service.response.ListAnnouncementResponse;
import com.cygnus.ipoten.studyroom.service.response.ReadAnnouncementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost", "http://localhost:9692"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study-rooms/{studyRoomId}/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;
    private final RedisCacheService redisCacheService;
    private final StudyRoomService studyRoomService;

    @PostMapping
    public ResponseEntity<CreateAnnouncementResponseForm> createAnnouncement(
            @PathVariable Long studyRoomId,
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody CreateAnnouncementRequestForm requestForm) {

        Long authorId = redisCacheService.getValueByKey(userToken, Long.class);

        String role = studyRoomService.findUserRoleInStudyRoom(studyRoomId, authorId);
        if (!"LEADER".equals(role)) {
            throw new IllegalStateException("리더만 공지사항을 작성할 수 있습니다.");
        }

        CreateAnnouncementResponse serviceResponse = announcementService.createAnnouncement(
                requestForm.toServiceRequest(studyRoomId, authorId)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateAnnouncementResponseForm.from(serviceResponse));
    }

    @GetMapping
    public ResponseEntity<List<ListAnnouncementResponseForm>> getAnnouncements(
            @PathVariable Long studyRoomId) {
        // 1. Service 호출하여 Service Response List 받기
        List<ListAnnouncementResponse> serviceResponse = announcementService.findAllAnnouncements(studyRoomId);

        // 2. Service Response List를 Controller Response Form List로 변환
        List<ListAnnouncementResponseForm> response = serviceResponse.stream()
                .map(ListAnnouncementResponseForm::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{announcementId}/pin")
    public ResponseEntity<Void> togglePin(
            @PathVariable Long studyRoomId,
            @PathVariable Long announcementId,
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);

        String role = studyRoomService.findUserRoleInStudyRoom(studyRoomId, currentUserId);
        if (!"LEADER".equals(role)) {
            throw new IllegalStateException("상단 고정 권한이 없는 사용자입니다.");
        }
        announcementService.toggleAnnouncementPin(studyRoomId, announcementId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{announcementId}")
    public ResponseEntity<ReadAnnouncementResponseForm> getAnnouncement(
            @PathVariable Long studyRoomId,
            @PathVariable Long announcementId) {

        ReadAnnouncementResponse serviceResponse = announcementService.findAnnouncementById(announcementId);
        ReadAnnouncementResponseForm response = ReadAnnouncementResponseForm.from(serviceResponse);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{announcementId}")
    public ResponseEntity<ReadAnnouncementResponseForm> updateAnnouncement(
            @PathVariable Long studyRoomId,
            @PathVariable Long announcementId,
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody UpdateAnnouncementRequestForm requestForm) {

        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);
        String role = studyRoomService.findUserRoleInStudyRoom(studyRoomId, currentUserId);
        if (!"LEADER".equals(role)) {
            throw new IllegalStateException("수정 권한이 없는 사용자입니다.");
        }

        ReadAnnouncementResponse serviceResponse = announcementService.updateAnnouncement(
                announcementId, requestForm.toServiceRequest()
        );

        return ResponseEntity.ok(ReadAnnouncementResponseForm.from(serviceResponse));
    }

    @DeleteMapping("/{announcementId}")
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable Long studyRoomId,
            @PathVariable Long announcementId,
            @CookieValue(name = "userToken", required = false) String userToken) {
        Long currentUserId = redisCacheService.getValueByKey(userToken, Long.class);
        String role = studyRoomService.findUserRoleInStudyRoom(studyRoomId, currentUserId);
        if (!"LEADER".equals(role)) {
            throw new IllegalStateException("삭제 권한이 없는 사용자입니다.");
        }
        announcementService.deleteAnnouncement(announcementId);
        return ResponseEntity.noContent().build();
    }
}