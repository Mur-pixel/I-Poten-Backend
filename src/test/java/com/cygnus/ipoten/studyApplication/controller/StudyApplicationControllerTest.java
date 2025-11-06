//package com.wowraid.jobspoon.studyApplication.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.wowraid.jobspoon.redis_cache.RedisCacheService;
//import com.wowraid.jobspoon.studyApplication.controller.request_form.CreateStudyApplicationRequestForm;
//import com.wowraid.jobspoon.studyApplication.entity.ApplicationStatus;
//import com.wowraid.jobspoon.studyApplication.service.StudyApplicationService;
//import com.wowraid.jobspoon.studyApplication.service.request.CreateStudyApplicationRequest;
//import com.wowraid.jobspoon.studyApplication.service.response.CreateStudyApplicationResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(StudyApplicationController.class)
//class StudyApplicationControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private StudyApplicationService studyApplicationService;
//
//    @MockBean
//    private RedisCacheService redisCacheService;
//
//    private final String FAKE_TOKEN = "Bearer fake-token-123";
//    private final Long FAKE_APPLICANT_ID = 1L;
//
//    @BeforeEach
//    void setUp() {
//        // Redis에서 정상적으로 사용자 ID를 찾아오는 상황을 Mocking
//        when(redisCacheService.getValueByKey(eq("fake-token-123"), eq(Long.class)))
//                .thenReturn(FAKE_APPLICANT_ID);
//    }
//
//    @Test
//    @DisplayName("스터디 지원 API 테스트 - 성공")
//    void applyToStudy_success() throws Exception {
//        // given
//        CreateStudyApplicationRequestForm requestForm = new CreateStudyApplicationRequestForm(14L, "열심히 하겠습니다.");
//
//        CreateStudyApplicationResponse serviceResponse = new CreateStudyApplicationResponse(
//                1L, ApplicationStatus.PENDING, LocalDateTime.now()
//        );
//
//        // Service가 성공적으로 응답을 반환하는 상황을 Mocking
//        when(studyApplicationService.applyToStudy(any(CreateStudyApplicationRequest.class)))
//                .thenReturn(serviceResponse);
//
//        // when & then
//        mockMvc.perform(post("/api/study-applications")
//                        .header("Authorization", FAKE_TOKEN)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestForm)))
//                .andExpect(status().isCreated()) // 201 Created 응답 확인
//                .andExpect(jsonPath("$.applicationId").value(1L))
//                .andExpect(jsonPath("$.status").value("PENDING"))
//                .andDo(print());
//    }
//
//    @Test
//    @DisplayName("스터디 지원 API 테스트 - 실패 (유효하지 않은 토큰)")
//    void applyToStudy_fail_invalidToken() throws Exception {
//        // given
//        CreateStudyApplicationRequestForm requestForm = new CreateStudyApplicationRequestForm(14L, "열심히 하겠습니다.");
//
//        // Redis에서 null을 반환하는(토큰이 없는) 상황을 Mocking
//        when(redisCacheService.getValueByKey(eq("invalid-token"), eq(Long.class)))
//                .thenReturn(null);
//
//        // when & then
//        mockMvc.perform(post("/api/study-applications")
//                        .header("Authorization", "Bearer invalid-token")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestForm)))
//                .andExpect(status().isUnauthorized()) // 401 Unauthorized 응답 확인
//                .andDo(print());
//    }
//}