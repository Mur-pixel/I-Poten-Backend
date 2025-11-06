//package com.wowraid.jobspoon.user_term.controller;
//
//import com.wowraid.jobspoon.redis_cache.RedisCacheService;
//import com.wowraid.jobspoon.user_term.entity.UserWordbookFolder;
//import com.wowraid.jobspoon.user_term.repository.UserTermProgressRepository;
//import com.wowraid.jobspoon.user_term.service.*;
//import com.wowraid.jobspoon.user_term.service.response.ListUserWordbookTermResponse;
//import jakarta.servlet.http.Cookie;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.*;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.util.Collections;
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * UserTermController의 단위 테스트
// * - 컨트롤러 레이어만 테스트 (WebMvcTest)
// * - 의존 서비스들은 모두 Mock 처리
// */
//@WebMvcTest(UserTermController.class)
//class UserTermControllerTest {
//
//    @MockBean private RedisCacheService redisCacheService;
//    @MockBean private com.wowraid.jobspoon.user_term.repository.UserWordbookFolderRepository userWordbookFolderRepository;
//    @MockBean private UserWordbookFolderService userWordbookFolderService;
//    @MockBean private UserWordbookTermService userWordbookTermService;
//    @MockBean private MemorizationService memorizationService;
//    @MockBean private UserRecentTermService userRecentTermService;
//    @MockBean private UserTermProgressRepository userTermProgressRepository;
//    @MockBean private UserWordbookFolderQueryService userWordbookFolderQueryService;
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    /**
//     * 특정 폴더의 단어장 목록을 페이징 조회하는 API 테스트
//     * - 인증된 사용자(쿠키)가 자신의 폴더에서 단어 목록을 페이징 조회
//     * - 페이징 파라미터(page, size, sort) 정상 처리 확인
//     * - 응답에 totalItems, totalPages 등 페이징 메타데이터 포함 확인
//     */
//    @Test
//    @DisplayName("GET /api/me/folders/{id}/terms - paged OK")
//    void list_terms_paged_ok() throws Exception {
//        // 1) 인증 쿠키 → accountId 변환 mock
//        given(redisCacheService.getValueByKey(eq("T1"), eq(Long.class))).willReturn(1L);
//
//        // 2) 폴더 소유권 확인 mock (사용자 ID 1이 폴더 소유자)
//        var folderMock = Mockito.mock(UserWordbookFolder.class, Mockito.RETURNS_DEEP_STUBS);
//        given(folderMock.getAccount().getId()).willReturn(1L);
//        given(userWordbookFolderRepository.findById(1L)).willReturn(Optional.of(folderMock));
//
//        // 3) 서비스 응답 mock: 총 123개 데이터, 빈 리스트 (첫 페이지)
//        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
//        Page<com.wowraid.jobspoon.user_term.entity.UserWordbookTerm> page =
//                new PageImpl<>(Collections.emptyList(), pageable, 123); // total=123
//
//        var serviceRes = ListUserWordbookTermResponse.from(page);
//        given(userWordbookFolderService.list(any())).willReturn(serviceRes);
//
//        // 4) API 호출 및 응답 검증
//        mockMvc.perform(get("/api/me/folders/{fid}/terms", 1L)
//                        .cookie(new Cookie("userToken", "T1"))
//                        .param("page", "0")
//                        .param("size", "20")
//                        .param("sort", "createdAt,DESC"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.userWordbookTermList").isArray())
//                .andExpect(jsonPath("$.totalItems").value(123))
//                // 총 페이지 수 계산 확인 (ceil(123/20)=7)
//                .andExpect(jsonPath("$.totalPages").value(7))
//                .andExpect(jsonPath("$.page").value(0))
//                .andExpect(jsonPath("$.size").value(20))
//                .andExpect(jsonPath("$.sort").value("createdAt,DESC"));
//    }
//}