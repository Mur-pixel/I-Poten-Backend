//package com.wowraid.jobspoon.quiz.controller;
//
//import com.wowraid.jobspoon.quiz.controller.response_form.SessionItemsPageResponseForm;
//import com.wowraid.jobspoon.quiz.controller.response_form.SessionListResponseForm;
//import com.wowraid.jobspoon.quiz.controller.response_form.SessionReviewResponseForm;
//import com.wowraid.jobspoon.quiz.controller.response_form.SessionSummaryResponseForm;
//import com.wowraid.jobspoon.quiz.entity.enums.SeedMode;
//import com.wowraid.jobspoon.quiz.entity.enums.SessionMode;
//import com.wowraid.jobspoon.quiz.entity.enums.SessionStatus;
//import com.wowraid.jobspoon.quiz.service.*;
//import com.wowraid.jobspoon.redis_cache.RedisCacheService;
//import com.wowraid.jobspoon.user_term.service.UserWordbookFolderQueryService;
//import jakarta.servlet.http.Cookie;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.util.List;
//
//import static org.hamcrest.Matchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(controllers = QuizController.class)
//class SessionQueryApiTest {
//
//    @Autowired
//    MockMvc mockMvc;
//
//    @MockBean UserQuizSessionQueryService userQuizSessionQueryService;
//    @MockBean RedisCacheService redisCacheService;
//    @MockBean QuizQuestionService quizQuestionService;
//    @MockBean QuizSetService quizSetService;
//    @MockBean QuizChoiceService quizChoiceService;
//    @MockBean UserQuizAnswerService userQuizAnswerService;
//    @MockBean UserWordbookFolderQueryService userWordbookFolderQueryService;
//
//    // 쿠키 없이 요청 시 401 반환
//    @Test
//    void unauthorized_withoutCookie_returns401() throws Exception {
//        mockMvc.perform(get("/api/me/quiz/sessions/1"))
//                .andExpect(status().isUnauthorized());
//    }
//
//    // 진행 중인 세션 요약 조회 성공
//    @Test
//    void summary_inProgress_ok() throws Exception {
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//
//        var summary = SessionSummaryResponseForm.builder()
//                .sessionId(1L)
//                .status(SessionStatus.IN_PROGRESS)
//                .totalCount(10)
//                .lastActivityAt(Instant.now())
//                .seedMode(SeedMode.FIXED)
//                .build();
//
//        when(userQuizSessionQueryService.getSummary(1L, 123L)).thenReturn(summary);
//
//        mockMvc.perform(get("/api/me/quiz/sessions/1")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
//                .andExpect(jsonPath("$.totalCount").value(10));
//    }
//
//    // 만료된 세션 요약 조회 성공
//    @Test
//    void summary_expired_ok() throws Exception {
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//
//        var summary = SessionSummaryResponseForm.builder()
//                .sessionId(1L)
//                .status(SessionStatus.EXPIRED)
//                .totalCount(7)
//                .lastActivityAt(Instant.now().minusSeconds(4000))
//                .seedMode(SeedMode.DAILY)
//                .build();
//
//        when(userQuizSessionQueryService.getSummary(1L, 123L)).thenReturn(summary);
//
//        mockMvc.perform(get("/api/me/quiz/sessions/1")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("EXPIRED"))
//                .andExpect(jsonPath("$.totalCount").value(7));
//    }
//
//    // 진행 중인 세션의 문제 목록 조회 시 정답 숨김
//    @Test
//    void items_inProgress_ok_answersHidden() throws Exception {
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//
//        var item = SessionItemsPageResponseForm.Item.builder()
//                .questionId(101L)
//                .questionText("Q1?")
//                .choices(List.of(
//                        SessionItemsPageResponseForm.Choice.builder()
//                                .id(1L).text("A").isAnswer(null).build(),
//                        SessionItemsPageResponseForm.Choice.builder()
//                                .id(2L).text("B").isAnswer(null).build()))
//                .build();
//
//        var page = SessionItemsPageResponseForm.builder()
//                .sessionId(1L)
//                .offset(0)
//                .limit(2)
//                .total(10)
//                .items(List.of(item))
//                .build();
//
//        when(userQuizSessionQueryService.getSessionItems(1L, 123L, 0, 2)).thenReturn(page);
//
//        mockMvc.perform(get("/api/me/quiz/sessions/1/items?offset=0&limit=2")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items[0].choices[0].isAnswer").value(nullValue()))
//                .andExpect(jsonPath("$.items[0].choices[1].isAnswer").value(nullValue()));
//    }
//
//    // 제출된 세션의 문제 목록 조회 시 정답 공개
//    @Test
//    void items_submitted_ok_answersRevealed() throws Exception {
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//
//        var item = SessionItemsPageResponseForm.Item.builder()
//                .questionId(201L)
//                .questionText("Q2?")
//                .choices(List.of(
//                        SessionItemsPageResponseForm.Choice.builder()
//                                .id(10L).text("A").isAnswer(true).build(),
//                        SessionItemsPageResponseForm.Choice.builder()
//                                .id(11L).text("B").isAnswer(false).build()))
//                .build();
//
//        var page = SessionItemsPageResponseForm.builder()
//                .sessionId(2L)
//                .offset(0)
//                .limit(2)
//                .total(2)
//                .items(List.of(item))
//                .build();
//
//        when(userQuizSessionQueryService.getSessionItems(2L, 123L, 0, 2)).thenReturn(page);
//
//        mockMvc.perform(get("/api/me/quiz/sessions/2/items?offset=0&limit=2")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items[0].choices[0].isAnswer").value(true))
//                .andExpect(jsonPath("$.items[0].choices[1].isAnswer").value(false));
//    }
//
//    // 만료된 세션의 문제 목록 조회 시 409 반환
//    @Test
//    void items_expired_conflict409() throws Exception {
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//        when(userQuizSessionQueryService.getSessionItems(1L, 123L, 0, 10))
//                .thenThrow(new IllegalStateException("만료된 세션 문제 조회는 금지됩니다."));
//
//        mockMvc.perform(get("/api/me/quiz/sessions/1/items?offset=0&limit=10")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isConflict());
//    }
//
//    // 문제 목록 조회 시 쿠키 없으면 401 반환
//    @Test
//    void items_unauthorized_withoutCookie_401() throws Exception {
//        mockMvc.perform(get("/api/me/quiz/sessions/1/items?offset=0&limit=10"))
//                .andExpect(status().isUnauthorized());
//    }
//
//    // 권한 없는 세션의 문제 목록 조회 시 404 반환
//    @Test
//    void items_securityException_mappedTo404() throws Exception {
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//        doThrow(new SecurityException("세션이 없거나 권한이 없습니다."))
//                .when(userQuizSessionQueryService).getSessionItems(999L, 123L, 0, 10);
//
//        mockMvc.perform(get("/api/me/quiz/sessions/999/items?offset=0&limit=10")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isNotFound());
//    }
//
//    // 존재하지 않거나 권한 없는 세션 요약 조회 시 404 반환
//    @Test
//    void notFound_or_forbidden_returns404() throws Exception {
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//        doThrow(new SecurityException("세션이 없거나 권한이 없습니다."))
//                .when(userQuizSessionQueryService).getSummary(999L, 123L);
//
//        mockMvc.perform(get("/api/me/quiz/sessions/999")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isNotFound());
//    }
//
//    /**
//     * 최근 세션 목록 조회 API 테스트
//     * GET /api/me/quiz/sessions?limit=20&status=ALL
//     *
//     * 검증 내용:
//     * - 쿠키 토큰으로 사용자 인증
//     * - 제출 완료(SUBMITTED)와 만료(EXPIRED) 세션 조회
//     * - 점수 자동 계산 확인 (17/20 = 85%)
//     */
//    @Test
//    void listMySessions_ok() throws Exception {
//        // Given: 토큰으로 userId=123 조회
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//
//        // Given: 2개의 세션 이력 데이터
//        var list = SessionListResponseForm.builder()
//                .items(List.of(
//                        // 제출 완료된 세션 (1시간 전 시작, 30분 전 제출)
//                        SessionListResponseForm.Item.builder()
//                                .sessionId(100L)
//                                .status(SessionStatus.SUBMITTED)
//                                .mode(SessionMode.FULL)
//                                .total(20)
//                                .correct(17)
//                                .startedAt(LocalDateTime.now().minusSeconds(3600))
//                                .submittedAt(LocalDateTime.now().minusSeconds(1800))
//                                .build(),
//                        // 만료된 세션 (2시간 전 시작, 미제출)
//                        SessionListResponseForm.Item.builder()
//                                .sessionId(99L)
//                                .status(SessionStatus.EXPIRED)
//                                .mode(null)
//                                .total(10)
//                                .startedAt(LocalDateTime.now().minusSeconds(7200))
//                                .submittedAt(null)
//                                .build()
//                ))
//                .build();
//
//        when(userQuizSessionQueryService.listMySessions(123L, 20, "ALL"))
//                .thenReturn(list);
//
//        // When & Then: API 호출 및 검증
//        mockMvc.perform(get("/api/me/quiz/sessions?limit=20&status=ALL")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.items", hasSize(2)))           // 총 2개 세션
//                .andExpect(jsonPath("$.items[0].sessionId").value(100L))
//                .andExpect(jsonPath("$.items[0].status").value("SUBMITTED"))
//                .andExpect(jsonPath("$.items[0].scorePercent").value(85))  // 17/20*100 = 85%
//                .andExpect(jsonPath("$.items[1].status").value("EXPIRED"));
//    }
//
//    /**
//     * 세션 리뷰 조회 API 테스트
//     * GET /api/me/quiz/sessions/{sessionId}/review
//     *
//     * 검증 내용:
//     * - 제출된 세션의 전체 문제와 정답/오답 확인
//     * - 각 문제의 보기 목록, 사용자 선택, 정답 여부 반환
//     * - 용어 정보와 카테고리 정보 포함
//     */
//    @Test
//    void review_ok() throws Exception {
//        // Given: 토큰으로 userId=123 조회
//        when(redisCacheService.getValueByKey("tok", Long.class)).thenReturn(123L);
//
//        // Given: 세션 리뷰 데이터 (1문제, 정답 맞춤)
//        var review = SessionReviewResponseForm.builder()
//                .sessionId(321L)
//                .status(SessionStatus.SUBMITTED)
//                .total(1)
//                .correct(1)
//                .items(List.of(
//                        SessionReviewResponseForm.Item.builder()
//                                .quizQuestionId(1L)
//                                .termId(10L)
//                                .termTitle("OSI 7계층")
//                                .categoryId(1000L)
//                                .categoryName("네트워크")
//                                .questionText("물리 계층은 무엇을 담당하나요?")
//                                .choices(List.of(
//                                        // 정답 보기
//                                        SessionReviewResponseForm.Choice.builder()
//                                                .id(501L).text("비트 전송").answer(true).build(),
//                                        // 오답 보기
//                                        SessionReviewResponseForm.Choice.builder()
//                                                .id(502L).text("경로 선택").answer(false).build()
//                                ))
//                                .myChoiceId(501L)  // 사용자가 501번(정답) 선택
//                                .explanation("물리 계층은 비트의 전송/수신을 담당합니다.")
//                                .correct(true)      // 정답 맞춤
//                                .build()
//                ))
//                .build();
//
//        when(userQuizSessionQueryService.getReview(321L, 123L))
//                .thenReturn(review);
//
//        // When & Then: API 호출 및 검증
//        mockMvc.perform(get("/api/me/quiz/sessions/321/review")
//                        .cookie(new Cookie("userToken", "tok")))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.sessionId").value(321L))
//                .andExpect(jsonPath("$.status").value("SUBMITTED"))
//                .andExpect(jsonPath("$.items[0].termTitle").value("OSI 7계층"))
//                .andExpect(jsonPath("$.items[0].choices[0].answer").value(true))  // 첫 번째 보기가 정답
//                .andExpect(jsonPath("$.items[0].myChoiceId").value(501L))         // 사용자 선택
//                .andExpect(jsonPath("$.items[0].correct").value(true));           // 정답 여부
//    }
//}