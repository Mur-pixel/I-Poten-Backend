//package com.wowraid.jobspoon.studyroom.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.wowraid.jobspoon.account.entity.Account;
//import com.wowraid.jobspoon.redis_cache.RedisCacheService;
//import com.wowraid.jobspoon.studyroom.controller.request_Form.CreateStudyRoomRequestForm;
//import com.wowraid.jobspoon.studyroom.controller.request_Form.UpdateStudyRoomRequestForm;
//import com.wowraid.jobspoon.studyroom.entity.StudyLevel;
//import com.wowraid.jobspoon.studyroom.entity.StudyLocation;
//import com.wowraid.jobspoon.studyroom.entity.StudyRoom;
//import com.wowraid.jobspoon.studyroom.service.StudyRoomService;
//import com.wowraid.jobspoon.studyroom.service.request.CreateStudyRoomRequest;
//import com.wowraid.jobspoon.studyroom.service.request.UpdateStudyRoomRequest;
//import com.wowraid.jobspoon.studyroom.service.response.CreateStudyRoomResponse;
//import com.wowraid.jobspoon.studyroom.service.response.ListStudyRoomResponse;
//import com.wowraid.jobspoon.studyroom.service.response.UpdateStudyRoomResponse;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.SliceImpl;
//import org.springframework.http.MediaType;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.List;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class StudyRoomControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private StudyRoomService studyRoomService;
//
//    @MockBean
//    private RedisCacheService redisCacheService;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    private final String FAKE_TOKEN = "Bearer fake-token-123";
//    private final Long FAKE_USER_ID = 1L;
//
//    @BeforeEach
//    void setUp() {
//        // 모든 테스트에서 Redis 조회를 Mocking
//        when(redisCacheService.getValueByKey(anyString(), eq(Long.class))).thenReturn(FAKE_USER_ID);
//    }
//
//    @Test
//    @DisplayName("스터디룸 생성 API 테스트")
//    void createStudyRoom() throws Exception {
//        // given
//        final CreateStudyRoomRequestForm requestForm = new CreateStudyRoomRequestForm(
//                "API 테스트 제목", "설명", 5, "SEOUL",
//                "NEWBIE", List.of("프론트엔드"), List.of("React")
//        );
//
//        // Service가 반환할 가짜 응답 DTO 생성
//        CreateStudyRoomResponse serviceResponse = new CreateStudyRoomResponse(
//                1L, "API 테스트 제목", "설명", 5, "RECRUITING", "SEOUL",
//                "NEWBIE", List.of("프론트엔드"), List.of("React"), LocalDateTime.now()
//        );
//
//        // Service의 createStudyRoom 메소드가 호출되면, 위에서 만든 응답을 반환하도록 설정
//        when(studyRoomService.createStudyRoom(any(CreateStudyRoomRequest.class)))
//                .thenReturn(serviceResponse);
//
//        // when & then
//        mockMvc.perform(post("/api/study-rooms")
//                        .header("Authorization", FAKE_TOKEN) // Authorization 헤더 추가
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestForm)))
//                .andExpect(status().isCreated()) // 201 Created 확인
//                .andExpect(jsonPath("$.applicationId").value(1L)) // 응답 Body의 내용 검증
//                .andExpect(jsonPath("$.title").value("API 테스트 제목"))
//                .andDo(print());
//    }
//
//    @Test
//    @DisplayName("스터디룸 목록 조회 API 테스트")
//    void getAllStudyRooms() throws Exception {
//        // given
//        ListStudyRoomResponse serviceResponse = new ListStudyRoomResponse(
//                new SliceImpl<>(Collections.emptyList())
//        );
//
//        when(studyRoomService.findAllStudyRooms(any())).thenReturn(serviceResponse);
//
//        // when & then
//        mockMvc.perform(get("/api/study-rooms")
//                .param("size", "10")
//                .param("lastStudyId", "100"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.hasNext").value(false))
//                .andExpect(jsonPath("$.studyRoomList").isArray());
//    }
//
//    @Test
//    @DisplayName("스터디모임 수정 API 테스트")
//    void updateStudyRoom() throws Exception {
//        // given
//        final Long studyRoomId = 1L;
//        UpdateStudyRoomRequestForm requestForm = new UpdateStudyRoomRequestForm(
//                "수정된 API 제목", "수정된 설명", 8, "BUSAN",
//                "SENIOR", List.of("풀스택"), List.of("Kotlin")
//        );
//
//        UpdateStudyRoomResponse serviceResponse = new UpdateStudyRoomResponse(
//                studyRoomId, "수정된 API 제목", "수정된 설명", 8, "RECRUITING", "BUSAN", "SENIOR",
//                List.of("풀스택"), List.of("Kotlin"), LocalDateTime.now()
//        );
//
//        when(studyRoomService.updateStudyRoom(eq(studyRoomId), eq(FAKE_USER_ID), any(UpdateStudyRoomRequest.class)))
//                .thenReturn(serviceResponse);
//
//        // when & then
//        mockMvc.perform(put("/api/study-rooms/{studyRoomId}", studyRoomId)
//                        .header("Authorization", FAKE_TOKEN) // Authorization 헤더 추가
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(requestForm)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.studyRoomId").value(studyRoomId))
//                .andExpect(jsonPath("$.title").value("수정된 API 제목"))
//                .andExpect(jsonPath("$.location").value("BUSAN"));
//    }
//
//    @Test
//    @DisplayName("스터디모임 삭제 API 테스트")
//    void deleteStudyRoom() throws Exception {
//        // given
//        final Long studyRoomId = 1L;
//
//        // when & then
//        mockMvc.perform(delete("/api/study-rooms/" + studyRoomId))
//                .andExpect(status().isNoContent());
//
//        verify(studyRoomService, times(1)).deleteStudyRoom(eq(studyRoomId), anyLong());
//    }
//}