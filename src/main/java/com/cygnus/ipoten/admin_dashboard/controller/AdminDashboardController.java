package com.cygnus.ipoten.admin_dashboard.controller;

import com.cygnus.ipoten.account.entity.AccountStatus;
import com.cygnus.ipoten.account.repository.AccountRepository;
import com.cygnus.ipoten.common.annotation.PublicEndpoint;
import com.cygnus.ipoten.common.util.InternalApiKeyValidator;
import com.cygnus.ipoten.interview.repository.InterviewRepository;
import com.cygnus.ipoten.quiz_session.repository.QuizSessionRepository;
import com.cygnus.ipoten.wordbook.repository.WordbookRepository;
import com.cygnus.ipoten.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AccountRepository accountRepository;
    private final InterviewRepository interviewRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final WordbookRepository wordbookRepository;
    private final InquiryRepository inquiryRepository;
    private final InternalApiKeyValidator internalApiKeyValidator;

    @PublicEndpoint
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if ("01026519025".equals(code)) {
            return ResponseEntity.ok(Map.of("verified", true));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("verified", false));
    }

    @PublicEndpoint
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "X-Admin-Code", required = false) String adminCode) {

        if (!"01026519025".equals(adminCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
        }

        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(kst);
        Instant todayStart = today.atStartOfDay(kst).toInstant();
        Instant weekAgo = today.minusDays(7).atStartOfDay(kst).toInstant();

        // User stats
        long totalUsers = accountRepository.count();
        long activeUsers = accountRepository.countByStatus(AccountStatus.ACTIVE);
        long newUsersToday = accountRepository.countByCreatedAtAfter(todayStart);
        long newUsersWeek = accountRepository.countByCreatedAtAfter(weekAgo);

        // Interview stats
        long totalInterviews = interviewRepository.count();

        // Quiz stats
        long totalQuizSessions = quizSessionRepository.count();

        // Wordbook stats
        long totalWordbooks = wordbookRepository.count();

        // Inquiry stats
        long totalInquiries = inquiryRepository.count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("users", Map.of(
                "total", totalUsers,
                "active", activeUsers,
                "newToday", newUsersToday,
                "newThisWeek", newUsersWeek
        ));
        stats.put("interviews", Map.of("total", totalInterviews));
        stats.put("quizSessions", Map.of("total", totalQuizSessions));
        stats.put("wordbooks", Map.of("total", totalWordbooks));
        stats.put("inquiries", Map.of("total", totalInquiries));
        stats.put("serverTime", Instant.now().toString());

        return ResponseEntity.ok(stats);
    }
}
