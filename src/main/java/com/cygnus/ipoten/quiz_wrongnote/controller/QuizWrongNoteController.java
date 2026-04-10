package com.cygnus.ipoten.quiz_wrongnote.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.quiz_wrongnote.controller.request_form.DeleteWrongNotesRequestForm;
import com.cygnus.ipoten.quiz_wrongnote.controller.request_form.WrongNoteResolvedUpdateRequestForm;
import com.cygnus.ipoten.quiz_wrongnote.service.QuizWrongNoteService;
import com.cygnus.ipoten.quiz_wrongnote.service.WrongNoteSearchCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class QuizWrongNoteController {

    private final QuizWrongNoteService quizWrongNoteService;

    @Operation(summary = "내 오답노트 목록 조회")
    @GetMapping("/me/quiz/reviews/wrong")
    public ResponseEntity<?> listWrongNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "문제 유형(CHOICE/OX/INITIALS 등)")
            @RequestParam(required = false) String type,
            @Parameter(description = "난이도(EASY/MEDIUM/HARD)")
            @RequestParam(required = false) String difficulty,
            @Parameter(description = "미해결만 조회")
            @RequestParam(defaultValue = "false") boolean unresolvedOnly,
            @Parameter(description = "검색어")
            @RequestParam(required = false) String q,
            @Parameter(description = "정렬(RECENT/OLDEST)")
            @RequestParam(defaultValue = "RECENT") String sort,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "true") boolean includeAnswers,
            @LoginUser Long accountId) {

        try {
            WrongNoteSearchCondition condition = WrongNoteSearchCondition.of(
                    q, type, difficulty, unresolvedOnly, sort, sessionId, from, to);
            var body = quizWrongNoteService.listWrongNotes(accountId, page, size, condition, includeAnswers);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("listWrongNotes failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "오답노트 해결 여부 변경")
    @PatchMapping("/me/quiz/reviews/{wrongNoteId}")
    public ResponseEntity<?> updateWrongNoteResolved(
            @PathVariable Long wrongNoteId,
            @RequestBody WrongNoteResolvedUpdateRequestForm requestForm,
            @LoginUser Long accountId) {

        if (requestForm == null || requestForm.getResolved() == null) {
            return ResponseEntity.badRequest().body("resolved 값이 필요합니다.");
        }

        try {
            quizWrongNoteService.updateResolved(accountId, wrongNoteId, requestForm.getResolved());
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("updateWrongNoteResolved failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "오답노트 항목 삭제")
    @DeleteMapping("/me/quiz/reviews/{wrongNoteId}")
    public ResponseEntity<?> deleteWrongNote(
            @PathVariable Long wrongNoteId,
            @LoginUser Long accountId) {

        try {
            quizWrongNoteService.deleteWrongNote(accountId, wrongNoteId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("deleteWrongNote failed wrongNoteId={}", wrongNoteId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/me/quiz/reviews/wrong")
    public ResponseEntity<?> deleteWrongNotesBulk(
            @RequestBody DeleteWrongNotesRequestForm requestForm,
            @LoginUser Long accountId) {

        if (requestForm == null || requestForm.getReviewIds() == null || requestForm.getReviewIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "reviewIds가 필요합니다."));
        }

        try {
            quizWrongNoteService.deleteWrongNotesBulk(accountId, requestForm.getReviewIds());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("deleteWrongNotesBulk failed reviewIds={}", requestForm.getReviewIds(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
