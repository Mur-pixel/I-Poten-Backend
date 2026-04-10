package com.cygnus.iptn.quiz_session.controller;

import com.cygnus.iptn.quiz_session.service.QuizSessionDeleteService;
import com.cygnus.iptn.quiz_session.service.QuizSessionRetryService;
import com.cygnus.iptn.quiz_session.service.QuizSessionQueryService;
import com.cygnus.iptn.quiz_session_scope.service.QuizScopeService;
import com.cygnus.iptn.redis_cache.RedisCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizSessionController.class)
class QuizSessionControllerDeleteTest {

    @Autowired MockMvc mockMvc;

    @MockBean RedisCacheService redisCacheService;
    @MockBean QuizSessionQueryService quizSessionQueryService;
    @MockBean QuizScopeService quizScopeService;
    @MockBean QuizSessionRetryService quizSessionRetryService;
    @MockBean QuizSessionDeleteService quizSessionDeleteService;

    @Test
    @DisplayName("쿠키 없으면 401")
    void delete_noCookie_401() throws Exception {
        mockMvc.perform(delete("/api/me/quiz/sessions/{id}", 10L))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(quizSessionDeleteService);
    }

    @Test
    @DisplayName("Redis에 토큰 없으면 401")
    void delete_tokenNotResolved_401() throws Exception {
        when(redisCacheService.getValueByKey(eq("t1"), eq(Long.class))).thenReturn(null);

        mockMvc.perform(delete("/api/me/quiz/sessions/{id}", 10L)
                        .cookie(new jakarta.servlet.http.Cookie("userToken", "t1")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(quizSessionDeleteService);
    }

    @Test
    @DisplayName("삭제 성공하면 204")
    void delete_success_204() throws Exception {
        when(redisCacheService.getValueByKey(eq("t1"), eq(Long.class))).thenReturn(7L);

        mockMvc.perform(delete("/api/me/quiz/sessions/{id}", 10L)
                        .cookie(new jakarta.servlet.http.Cookie("userToken", "t1"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(quizSessionDeleteService).deleteMySession(7L, 10L);
    }

    @Test
    @DisplayName("세션 없음/내 세션 아님이면 404")
    void delete_notFound_404() throws Exception {
        when(redisCacheService.getValueByKey(eq("t1"), eq(Long.class))).thenReturn(7L);
        doThrow(new NoSuchElementException()).when(quizSessionDeleteService).deleteMySession(7L, 10L);

        mockMvc.perform(delete("/api/me/quiz/sessions/{id}", 10L)
                        .cookie(new jakarta.servlet.http.Cookie("userToken", "t1")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("기타 예외면 500")
    void delete_error_500() throws Exception {
        when(redisCacheService.getValueByKey(eq("t1"), eq(Long.class))).thenReturn(7L);
        doThrow(new RuntimeException("boom")).when(quizSessionDeleteService).deleteMySession(7L, 10L);

        mockMvc.perform(delete("/api/me/quiz/sessions/{id}", 10L)
                        .cookie(new jakarta.servlet.http.Cookie("userToken", "t1")))
                .andExpect(status().isInternalServerError());
    }
}
