package com.cygnus.ipoten.report.service;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.report.entity.Report;
import com.cygnus.ipoten.report.entity.ReportStatus;
import com.cygnus.ipoten.report.entity.ReportType;
import com.cygnus.ipoten.report.repository.ReportRepository;
import com.cygnus.ipoten.report.service.request.CreateReportRequest;
import com.cygnus.ipoten.report.service.response.CreateReportResponse;
import com.cygnus.ipoten.report.service.response.UploadUrlResponse;
import com.cygnus.ipoten.studyroom.entity.StudyRoom;
import com.cygnus.ipoten.studyroom.repository.StudyRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final AccountProfileRepository accountProfileRepository;
    private final StudyRoomRepository studyRoomRepository;
    private final ReportS3Service reportS3Service;

    @Override
    public void createReport(CreateReportRequest request, Long reporterId) {
        // 1. 신고자, 신고 대상자, 스터디룸 정보 조회
        AccountProfile reporter = accountProfileRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("신고자를 찾을 수 없습니다."));
        AccountProfile reportedUser = accountProfileRepository.findById(request.getReportedUserId())
                .orElseThrow(() -> new IllegalArgumentException("신고 대상자를 찾을 수 없습니다."));
        StudyRoom studyRoom = studyRoomRepository.findById(request.getStudyRoomId())
                .orElseThrow(() -> new IllegalArgumentException("스터디룸을 찾을 수 없습니다."));

        // 2. 자기 자신을 신고하는 경우 방지
        if (reporter.getId().equals(reportedUser.getId())) {
            throw new IllegalStateException("자기 자신을 신고할 수 없습니다.");
        }

        // 3. 중복 신고 방지 로직
        List<ReportStatus> checkStatuses = List.of(ReportStatus.PENDING, ReportStatus.IN_PROGRESS);

        boolean isDuplicate = reportRepository.existsByReporterAndReportedUserAndStatusIn(
                reporter, reportedUser, checkStatuses
        );

        if (isDuplicate) {
            throw new IllegalStateException("이미 해당 사용자에 대해 처리 중인 신고가 존재합니다.");
        }

        // 4. 신고 엔티티 생성 및 저장
        Report newReport = Report.create(
                reporter,
                reportedUser,
                ReportType.STUDY_ROOM,      // StudyRoom 객체 대신 ReportType Enum 전달
                studyRoom.getId(),          // studyRoom의 ID를 sourceId로 전달
                request.getCategory(),
                request.getDescription(),
                request.getAttachmentS3Key()
        );
        reportRepository.save(newReport);
    }

    // Presigned URL 생성 로직
    @Override
    public UploadUrlResponse generateUploadUrl(Long reporterId, String filename) {
        String extension = "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex);
        }

        // S3에 저장될 고유한 키 생성
        String key = String.format("reports/%d/%s%s", reporterId, UUID.randomUUID(), extension);

        // contentType은 프론트에서 파일 업로드 시 지정하도록 null로 두거나, 기본값을 지정할 수 있습니다.
        String uploadUrl = reportS3Service.generateUploadUrl(key, null);

        return new UploadUrlResponse(uploadUrl, key);
    }

    // 신고목록 조회 구현
    @Override
    @Transactional(readOnly = true)
    public List<CreateReportResponse> findAllReports() {
        return reportRepository.findAll().stream()
                .map(CreateReportResponse::from) // public static 'from' 메소드를 사용하도록 수정
                .collect(Collectors.toList());
    }

    // 신고상태 변경 로직
    @Override
    public void updateReportStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 신고를 찾을 수 없습니다.: " + reportId));

        report.updateStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreateReportResponse> findReportsByReporter(Long reporterId) {
        List<Report> reports = reportRepository.findAllByReporter_Id(reporterId);
        return reports.stream()
                .map(CreateReportResponse::from)
                .collect(Collectors.toList());
    }
}