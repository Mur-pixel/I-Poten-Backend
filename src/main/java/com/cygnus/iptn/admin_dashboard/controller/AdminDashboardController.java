package com.cygnus.iptn.admin_dashboard.controller;

import com.cygnus.iptn.account.entity.AccountStatus;
import com.cygnus.iptn.account.repository.AccountRepository;
import com.cygnus.iptn.common.annotation.PublicEndpoint;
import com.cygnus.iptn.common.util.InternalApiKeyValidator;
import com.cygnus.iptn.inquiry.entity.Inquiry;
import com.cygnus.iptn.inquiry.repository.InquiryRepository;
import com.cygnus.iptn.interview.repository.InterviewRepository;
import com.cygnus.iptn.interview_review.entity.InterviewReview;
import com.cygnus.iptn.interview_review.repository.InterviewReviewRepository;
import com.cygnus.iptn.iptn_review.entity.IptnReview;
import com.cygnus.iptn.iptn_review.repository.IptnReviewRepository;
import com.cygnus.iptn.quiz_session.repository.QuizSessionRepository;
import com.cygnus.iptn.wordbook.repository.WordbookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final IptnReviewRepository iptnReviewRepository;
    private final InterviewReviewRepository interviewReviewRepository;
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

    @PublicEndpoint
    @GetMapping("/reviews")
    public ResponseEntity<?> getReviews(
            @RequestHeader(value = "X-Admin-Code", required = false) String adminCode) {

        if (!"01026519025".equals(adminCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
        }

        // Service reviews (iptn_review)
        List<Map<String, Object>> serviceReviews = iptnReviewRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("type", "SERVICE");
                    m.put("rating", r.getRating());
                    m.put("comment", r.getComment());
                    m.put("accountId", r.getAccount() != null ? r.getAccount().getId() : null);
                    m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                    return m;
                })
                .collect(Collectors.toList());

        // Interview reviews
        List<Map<String, Object>> interviewReviews = interviewReviewRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("type", "INTERVIEW");
                    m.put("rating", r.getRating());
                    m.put("comment", r.getComment());
                    m.put("accountId", r.getAccount() != null ? r.getAccount().getId() : null);
                    m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "serviceReviews", serviceReviews,
                "interviewReviews", interviewReviews
        ));
    }

    @PublicEndpoint
    @GetMapping("/inquiries")
    public ResponseEntity<?> getInquiries(
            @RequestHeader(value = "X-Admin-Code", required = false) String adminCode) {

        if (!"01026519025".equals(adminCode)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
        }

        List<Map<String, Object>> inquiries = inquiryRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(inq -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", inq.getId());
                    m.put("type", inq.getType().name());
                    m.put("title", inq.getTitle());
                    m.put("content", inq.getContent());
                    m.put("status", inq.getStatus().name());
                    m.put("answerContent", inq.getAnswerContent());
                    m.put("answeredAt", inq.getAnsweredAt() != null ? inq.getAnsweredAt().toString() : null);
                    m.put("accountId", inq.getAccount() != null ? inq.getAccount().getId() : null);
                    m.put("createdAt", inq.getCreatedAt() != null ? inq.getCreatedAt().toString() : null);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("inquiries", inquiries));
    }
}
