package com.cygnus.ipoten.report.controller;

import com.cygnus.ipoten.account.entity.Account;
import com.cygnus.ipoten.account.entity.RoleType;
import com.cygnus.ipoten.accountProfile.entity.AccountProfile;
import com.cygnus.ipoten.accountProfile.repository.AccountProfileRepository;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.report.service.ReportService;
import com.cygnus.ipoten.report.service.request.UpdateReportStatusRequest;
import com.cygnus.ipoten.report.service.response.CreateReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminReportController {

    private final ReportService reportService;
    private final RedisCacheService redisCacheService;
    private final AccountProfileRepository accountProfileRepository;

    @GetMapping("/study-rooms/reports")
    public ResponseEntity<List<CreateReportResponse>> getStudyRoomReports(
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long userId = redisCacheService.getValueByKey(userToken, Long.class);
        if(userId == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AccountProfile userProfile = accountProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Account account = userProfile.getAccount();
        if (account.getAccountRoleType().getRoleType() != RoleType.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<CreateReportResponse> reports = reportService.findAllReports();
        return ResponseEntity.ok(reports);
    }

    @PatchMapping("/study-rooms/reports/{reportId}/status")
    public ResponseEntity<Void> updateReportStatus(
            @PathVariable Long reportId,
            @RequestBody UpdateReportStatusRequest request,
            @CookieValue(name = "userToken", required = false) String userToken) {

        Long userId = redisCacheService.getValueByKey(userToken, Long.class);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AccountProfile userProfile = accountProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (userProfile.getAccount().getAccountRoleType().getRoleType() != RoleType.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        reportService.updateReportStatus(reportId, request.getStatus());

        return ResponseEntity.ok().build();
    }
}
