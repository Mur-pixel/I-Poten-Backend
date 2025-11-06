package com.cygnus.ipoten.report.controller;

import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.report.service.ReportService;
import com.cygnus.ipoten.report.service.request.CreateReportRequest;
import com.cygnus.ipoten.report.service.response.UploadUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/study-rooms/reports")
public class StudyRoomReportController {

    private final ReportService reportService;
    private final RedisCacheService redisCacheService;

    @PostMapping
    public ResponseEntity<?> createReport(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody CreateReportRequest request) {

        try {
            Long reporterId = redisCacheService.getValueByKey(userToken, Long.class);
            reportService.createReport(request, reporterId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalStateException e) {
            // 예외가 발생하면, 프론트가 원하는 JSON 형식으로 에러 메시지를 만들어 반환
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Presigned URL 발급 엔드포인트
    @PostMapping("/upload-url")
    public ResponseEntity<UploadUrlResponse> getUploadUrl(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestBody Map<String, String> payload) {

        Long reporterId = redisCacheService.getValueByKey(userToken, Long.class);
        String filename = payload.get("filename");

        UploadUrlResponse response = reportService.generateUploadUrl(reporterId, filename);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyReports(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long reporterId = redisCacheService.getValueByKey(userToken, Long.class);
        return ResponseEntity.ok(reportService.findReportsByReporter(reporterId));
    }
}