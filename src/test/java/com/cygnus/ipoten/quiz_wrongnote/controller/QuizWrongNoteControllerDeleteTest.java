package com.cygnus.ipoten.quiz_wrongnote.controller;

import com.cygnus.ipoten.quiz_wrongnote.service.QuizWrongNoteService;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizWrongNoteController.class)
class QuizWrongNoteControllerDeleteTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RedisCacheService redisCacheService;
    @MockitoBean QuizWrongNoteService quizWrongNoteService;

    @Test
    @DisplayName("쿠키 없으면 401")
    void delete_noCookie_401() throws Exception {
        mockMvc.perform(delete("/api/me/quiz/reviews/{id}", 10L))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(quizWrongNoteService);
    }

    @Test
    @DisplayName("Redis에 토큰 없으면 401")
    void delete_tokenNotResolved_401() throws Exception {
        when(redisCacheService.getValueByKey(eq("t1"), eq(Long.class))).thenReturn(null);

        mockMvc.perform(delete("/api/me/quiz/reviews/{id}", 10L)
                        .cookie(new Cookie("userToken", "t1"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(quizWrongNoteService);
    }

    @Test
    @DisplayName("삭제 성공하면 204")
    void delete_success_204() throws Exception {
        when(redisCacheService.getValueByKey(eq("t1"), eq(Long.class))).thenReturn(7L);

        mockMvc.perform(delete("/api/me/quiz/reviews/{id}", 10L)
                        .cookie(new Cookie("userToken", "t1")))
                .andExpect(status().isNoContent());

        verify(quizWrongNoteService).deleteWrongNote(7L, 10L);
    }

    @Test
    @DisplayName("없음/내것 아님이면 404")
    void delete_notFound_404() throws Exception {
        when(redisCacheService.getValueByKey(eq("t1"), eq(Long.class))).thenReturn(7L);
        doThrow(new NoSuchElementException()).when(quizWrongNoteService).deleteWrongNote(7L, 10L);

        mockMvc.perform(delete("/api/me/quiz/reviews/{id}", 10L)
                        .cookie(new Cookie("userToken", "t1")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("기타 예외면 500")
    void delete_error_500() throws Exception {
        when(redisCacheService.getValueByKey(eq("t1"), eq(Long.class))).thenReturn(7L);
        doThrow(new RuntimeException("boom")).when(quizWrongNoteService).deleteWrongNote(7L, 10L);

        mockMvc.perform(delete("/api/me/quiz/reviews/{id}", 10L)
                        .cookie(new Cookie("userToken", "t1")))
                .andExpect(status().isInternalServerError());
    }
}