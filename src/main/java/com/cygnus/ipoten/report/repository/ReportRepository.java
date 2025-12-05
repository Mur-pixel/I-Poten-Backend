package com.cygnus.ipoten.report.repository;

import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.report.entity.Report;
import com.cygnus.ipoten.report.entity.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    // 특정 사용자에 대한 처리중인 신고가 있는지 확인 == 중복 신고방지
    boolean existsByReporterAndReportedUserAndStatusIn(
            AccountProfile reporter, AccountProfile reportedUser, List<ReportStatus> statuses
    );

    // [추가] 특정 상태의 모든 신고 목록을 조회하는 기능
    List<Report> findAllByStatus(ReportStatus status);


    void deleteAllByReporterIdOrReportedUserId(Long reporterId, Long reportedUserId);
    List<Report> findAllByReporter_Id(Long reporterId);
  
  
}