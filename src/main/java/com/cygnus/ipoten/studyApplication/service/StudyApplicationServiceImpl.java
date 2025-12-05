package com.cygnus.ipoten.studyApplication.service;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.studyApplication.entity.ApplicationStatus;
import com.cygnus.ipoten.studyApplication.entity.StudyApplication;
import com.cygnus.ipoten.studyApplication.repository.StudyApplicationRepository;
import com.cygnus.ipoten.studyApplication.service.request.CreateStudyApplicationRequest;
import com.cygnus.ipoten.studyApplication.service.request.ProcessApplicationRequest;
import com.cygnus.ipoten.studyApplication.service.response.ApplicationForHostResponse;
import com.cygnus.ipoten.studyApplication.service.response.CreateStudyApplicationResponse;
import com.cygnus.ipoten.studyApplication.service.response.ListMyApplicationResponse;
import com.cygnus.ipoten.studyApplication.service.response.MyApplicationStatusResponse;
import com.cygnus.ipoten.studyroom.entity.StudyMember;
import com.cygnus.ipoten.studyroom.entity.StudyRole;
import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import com.cygnus.ipoten.studyroom.entity.StudyStatus;
import com.cygnus.ipoten.studyroom.repository.StudyMemberRepository;
import com.cygnus.ipoten.studyroom.repository.StudyRoomRepository;
import com.cygnus.ipoten.studyroom.service.StudyRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudyApplicationServiceImpl implements StudyApplicationService {

    private final StudyApplicationRepository studyApplicationRepository;
    private final StudyRoomRepository studyRoomRepository;
    private final AccountProfileRepository accountProfileRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRoomService studyRoomService;

    @Override
    public CreateStudyApplicationResponse applyToStudy(CreateStudyApplicationRequest request) {
        log.info("applyToStudy 서비스 시작. studyRoomId={}, applicantId={}, message='{}'",
                request.getStudyRoomId(), request.getApplicantId(), request.getMessage());

        AccountProfile applicant = accountProfileRepository.findById(request.getApplicantId())
                .orElseThrow(() -> new IllegalArgumentException("해당 지원자를 찾을 수 없습니다. ID: " + request.getApplicantId()));

        StudyRoom studyRoom = studyRoomRepository.findById(request.getStudyRoomId())
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디모임을 찾을 수 없습니다. ID: " + request.getStudyRoomId()));

        if (studyRoom.getStatus() == StudyStatus.CLOSED) {
            throw new IllegalStateException("이미 마감되거나 폐쇄된 스터디모임입니다.");
        }

        if (studyRoom.getHost().getId().equals(applicant.getId())) {
            throw new IllegalStateException("모임장은 자신의 스터디에 지원할 수 없습니다.");
        }

        boolean isAlreadyApplied = studyApplicationRepository.existsByStudyRoomAndApplicant(studyRoom, applicant);
        if (isAlreadyApplied) {
            throw new IllegalStateException("이미 해당 스터디모임에 지원했습니다.");
        }

        StudyApplication studyApplication = StudyApplication.create(
                studyRoom,
                applicant,
                request.getMessage()
        );

        StudyApplication savedApplication = studyApplicationRepository.save(studyApplication);

        return CreateStudyApplicationResponse.from(savedApplication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListMyApplicationResponse> findMyApplications(Long applicantId) {
        return studyApplicationRepository.findAllByApplicantIdWithStudyRoom(applicantId)
                .stream()
                .map(ListMyApplicationResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelApplication(Long applicationId, Long applicantId) {

        // 취소할 신청 내역을 ID로 조회함
        StudyApplication application = studyApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청 내역입니다." + applicationId));

        // 신청한 본인이 맞는지 권한 확인
        if (!application.getApplicant().getId().equals(applicantId)) {
            throw new IllegalStateException("신청을 취소할 권한이 없습니다.");
        }

        // 대기중(PENDDING) 상태일 때만 취소 가능하도록 제한을 둠
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기중인 신청만 취소할 수 있습니다.");
        }

        // 권한검사 후 신청내역 삭제
        studyApplicationRepository.delete(application);
    }

    @Override
    public MyApplicationStatusResponse findMyApplicationStatus(Long studyRoomId, Long applicantId) {
        if (applicantId == null) {
            return new MyApplicationStatusResponse(null, ApplicationStatus.NOT_APPLIED);
        }

        StudyRoom studyRoom = studyRoomRepository.findById(studyRoomId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스터디모임을 찾을 수 없습니다."));
        AccountProfile applicant = accountProfileRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지원자를 찾을 수 없습니다."));

        return studyApplicationRepository.findByStudyRoomAndApplicant(studyRoom, applicant)
                .map(application -> new MyApplicationStatusResponse(application.getId(), application.getStatus()))
                .orElse(new MyApplicationStatusResponse(null, ApplicationStatus.NOT_APPLIED));
    }

    // 스터디모임의 호스트가 해당 스터디모임에게 들어온 참가신청 리스트를 조회하는 비즈니스 로직
    @Override
    @Transactional(readOnly = true)
    public List<ApplicationForHostResponse> findApplicationsForHost(Long studyRoomId, Long hostId) {
        StudyRoom studyRoom = studyRoomRepository.findById(studyRoomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디모임입니다."));
        if (!studyRoom.getHost().getId().equals(hostId)) {
            throw new IllegalStateException("스터디모임 호스트만 신청 내역을 조회할 수 있습니다.");
        }

        return studyApplicationRepository.findAllByStudyRoomId(studyRoomId).stream()
                .map(ApplicationForHostResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void processApplication(Long applicationId, Long hostId, ProcessApplicationRequest request) {
        StudyApplication application = studyApplicationRepository.findByIdWithAllDetails(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));

        if (application.getStudyRoom().getStatus() == StudyStatus.CLOSED) {
            throw new IllegalStateException("폐쇄된 스터디모임의 지원서는 처리할 수 없습니다.");
        }

        // 권한 검사: 요청자가 해당 스터디의 호스트인지 확인
        if (!application.getStudyRoom().getHost().getId().equals(hostId)) {
            throw new IllegalStateException("스터디 호스트만 신청을 처리할 수 있습니다.");
        }

        // 상태 검사: '대기중'인 신청만 처리 가능
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new IllegalStateException("대기중인 신청만 처리할 수 있습니다.");
        }

        ApplicationStatus newStatus = request.getStatus();
        application.updateStatus(newStatus); // 신청 상태(status) 변경

        // 수락(APPROVED)인 경우에만 study_member 테이블에 멤버로 추가
        if (newStatus == ApplicationStatus.APPROVED) {
            StudyRoom studyRoom = application.getStudyRoom();

            // 멤버를 추가하기 전에 인원이 꽉 찼는지 확인합니다.
            if (studyRoom.getStudyMembers().size() >= studyRoom.getMaxMembers()) {
                // 인원이 꽉 찼다면, 에러를 발생시켜 작업을 중단합니다.
                throw new IllegalStateException("모집 인원이 마감되어 더 이상 수락할 수 없습니다.");
            } else {
                // 인원이 꽉 차지 않았을 때만 기존 로직을 실행합니다.
                StudyMember newMember = StudyMember.create(
                        studyRoom,
                        application.getApplicant(),
                        StudyRole.MEMBER
                );
                studyRoom.addStudyMember(newMember);
                studyRoomService.updateStudyRoomStatusBasedOnMemberCount(studyRoom);
            }
        }
    }
}