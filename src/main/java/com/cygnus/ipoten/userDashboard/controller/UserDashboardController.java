package com.cygnus.ipoten.userDashboard.controller;

import com.cygnus.ipoten.userAttendance.service.AttendanceService;
import com.cygnus.ipoten.userDashboard.controller.response.*;
import com.cygnus.ipoten.userDashboard.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-dashboard")
@RequiredArgsConstructor
public class UserDashboardController {

    private final TokenAccountService tokenAccountService;
    private final AttendanceService attendanceService;
    private final QuizSummaryService quizSummaryService;
    private final WritingCountService writingCountService;
    private final InterviewSummaryService interviewSummaryService;

    /**
     * 이번 달 출석률 조회
     * - attended: 이번 달 출석 일수
     * - totalDays: 이번 달 총 일수
     * - attendanceRate: 출석률 (attended / totalDays)
     */
    @GetMapping("/attendance/rate")
    public ResponseEntity<AttendanceRateResponse> getRate(@CookieValue(name = "userToken", required = false) String userToken){

        Long accountId = tokenAccountService.resolveAccountId(userToken);
        AttendanceRateResponse response = attendanceService.getThisMonthRate(accountId);

        return ResponseEntity.ok(response);
    }

    /**
     * 문제풀이(퀴즈) 완료 현황 조회
     * - totalCount: 누적 완료 수
     * - monthlyCount: 이번 달 완료 수
     */
    @GetMapping("/quiz/completion")
    public ResponseEntity<QuizCompletionResponse> getQuizCompletion(@CookieValue(name = "userToken", required = false) String userToken){
        Long accountId = tokenAccountService.resolveAccountId(userToken);
        QuizCompletionResponse response = new QuizCompletionResponse(
                quizSummaryService.getTotalCount(accountId),
                quizSummaryService.getMonthlyCount(accountId)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 글쓰기 활동 통계 조회
     * - posts: 게시글 작성 수
     * - studyrooms: 스터디룸 개설 수
     * - comments: 댓글 작성 수
     * - total: 전체 합계 (posts + studyrooms + comments)
     */
    @GetMapping("/writing/count")
    public ResponseEntity<WritingCountResponse> getWritingCount(@CookieValue(name = "userToken", required = false) String userToken){
        Long accountId = tokenAccountService.resolveAccountId(userToken);

        WritingCountResponse response = new WritingCountResponse(
                writingCountService.getStudyroomsCount(accountId)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 인터뷰(모의면접) 통계 조회
     * - total: 총 완료된 인터뷰 수
     * - monthly: 이번 달 완료된 인터뷰 수
     */
    @GetMapping("/interview/completion")
    public ResponseEntity<InterviewCompletionResponse> getInterviewSummary(
            @CookieValue(name = "userToken", required = false) String userToken
    ) {
        Long accountId = tokenAccountService.resolveAccountId(userToken);

        InterviewCompletionResponse response = new InterviewCompletionResponse(
                interviewSummaryService.getTotalFinishedCount(accountId),
                interviewSummaryService.getMonthlyFinishedCount(accountId)
        );

        return ResponseEntity.ok(response);
    }
}