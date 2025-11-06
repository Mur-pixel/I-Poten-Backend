//package com.wowraid.jobspoon.user_term.controller;
//
//import com.wowraid.jobspoon.redis_cache.RedisCacheService;
//import com.wowraid.jobspoon.user_term.service.UserWordbookTermService;
//import com.wowraid.jobspoon.user_term.service.MemorizationService;
//import com.wowraid.jobspoon.user_term.service.UserRecentTermService;
//import com.wowraid.jobspoon.user_term.service.UserWordbookFolderQueryService;
//import com.wowraid.jobspoon.user_term.service.UserWordbookFolderService;
//import com.wowraid.jobspoon.user_term.repository.UserTermProgressRepository;
//import com.wowraid.jobspoon.user_term.repository.UserWordbookFolderRepository;
//import jakarta.servlet.http.Cookie;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.HttpStatus;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.web.server.ResponseStatusException;
//
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * 사용자 단어장 폴더의 용어 개수 조회 API 테스트
// * GET /api/me/folders/{folderId}/terms/count
// */
//@WebMvcTest(UserTermController.class)
//class FolderCountControllerTest {
//
//    @Autowired MockMvc mockMvc;
//
//    @MockBean
//    UserWordbookTermService userWordbookTermService;
//    @MockBean UserWordbookFolderService userWordbookFolderService;
//    @MockBean MemorizationService memorizationService;
//    @MockBean UserWordbookFolderRepository userWordbookFolderRepository;
//    @MockBean UserRecentTermService userRecentTermService;
//    @MockBean RedisCacheService redisCacheService;
//    @MockBean UserTermProgressRepository userTermProgressRepository;
//    @MockBean UserWordbookFolderQueryService userWordbookFolderQueryService;
//
//    @Test
//    @DisplayName("200 OK: 폴더 총 개수를 반환")
//    void count_ok_200() throws Exception {
//        // given: 유효한 토큰과 폴더에 3개의 용어가 있음
//        when(redisCacheService.getValueByKey("T", Long.class)).thenReturn(1L);
//        when(userWordbookFolderQueryService.countTermsInFolderOrThrow(1L, 10L)).thenReturn(3L);
//
//        // when & then: 정상적으로 폴더 ID와 개수를 반환
//        mockMvc.perform(get("/api/me/folders/10/terms/count").cookie(new Cookie("userToken", "T")))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.folderId", is(10)))
//                .andExpect(jsonPath("$.count", is(3)));
//    }
//
//    @Test
//    @DisplayName("401 Unauthorized: 인증 실패 시")
//    void count_unauthorized_401() throws Exception {
//        // given: 유효하지 않은 토큰
//        when(redisCacheService.getValueByKey("BAD", Long.class)).thenReturn(null);
//
//        // when & then: 401 Unauthorized 응답
//        mockMvc.perform(get("/api/me/folders/10/terms/count").cookie(new Cookie("userToken", "BAD")))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    @DisplayName("404 Not Found: 없는 폴더 또는 소유하지 않은 폴더")
//    void count_notfound_404() throws Exception {
//        // given: 존재하지 않거나 소유하지 않은 폴더 조회 시 예외 발생
//        when(redisCacheService.getValueByKey("T", Long.class)).thenReturn(1L);
//        when(userWordbookFolderQueryService.countTermsInFolderOrThrow(anyLong(), anyLong()))
//                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다."));
//
//        // when & then: 404 Not Found 응답
//        mockMvc.perform(get("/api/me/folders/9999/terms/count").cookie(new Cookie("userToken", "T")))
//                .andExpect(status().isNotFound());
//    }
//}