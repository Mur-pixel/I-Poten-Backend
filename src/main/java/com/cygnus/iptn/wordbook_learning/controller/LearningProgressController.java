package com.cygnus.iptn.wordbook_learning.controller;

import com.cygnus.iptn.common.annotation.LoginUser;
import com.cygnus.iptn.wordbook_log.service.WordbookLogService;
import com.cygnus.iptn.wordbook_learning.service.LearningProgressService;
import com.cygnus.iptn.wordbook_learning.controller.request_form.UpdateLearningProgressRequestForm;
import com.cygnus.iptn.wordbook_learning.controller.response_form.UpdateLearningProgressResponseForm;
import com.cygnus.iptn.wordbook_learning.repository.LearningProgressRepository;
import com.cygnus.iptn.wordbook.service.WordbookQueryService;
import com.cygnus.iptn.wordbook_learning.service.request.UpdateLearningProgressRequest;
import com.cygnus.iptn.wordbook_learning.service.response.UpdateLearningProgressResponse;
import com.cygnus.iptn.wordbook_term.repository.WordbookTermRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "UserTerm", description = "사용자 단어장에 있는 단어들 학습 상태 관리 API")
public class LearningProgressController {

    private final LearningProgressService learningProgressService;
    private final WordbookQueryService wordbookQueryService;
    private final LearningProgressRepository learningProgressRepository;
    private final WordbookLogService wordbookLogService;
    private final WordbookTermRepository wordbookTermRepository;

    @Operation(summary = "단어 학습 상태 변경")
    @PatchMapping("/me/terms/{termId}/memorization")
    public UpdateLearningProgressResponseForm updateMemorizationByTermId(
            @LoginUser Long accountId,
            @PathVariable Long termId,
            @RequestBody @Valid UpdateLearningProgressRequestForm requestForm) {

        UpdateLearningProgressRequest request = requestForm.toUpdateMemorizationRequest(accountId, termId);
        UpdateLearningProgressResponse response = learningProgressService.updateMemorization(request);

        Long wordbookId = null;
        try {
            wordbookId = wordbookTermRepository.findMinWordbookIdByAccountIdAndTermId(accountId, termId);
        } catch (Exception e) {
            log.debug("[wordbook_log] wordbookId resolve failed (ignored). termId={}", termId, e);
        }

        try {
            wordbookLogService.recordMemoChanged(accountId, wordbookId, termId, response.getStatus().name());
        } catch (Exception e) {
            log.warn("[wordbook_log] MEMO_STATUS_CHANGED failed (ignored). termId={}", termId, e);
        }

        return UpdateLearningProgressResponseForm.from(response);
    }

    @Operation(summary = "여러 단어의 학습 상태 일괄 조회")
    @GetMapping("/me/terms/memorization")
    public Map<String, String> getMemorizationStatuses(
            @LoginUser Long accountId,
            @RequestParam(name = "ids") String idsCsv) {

        if (idsCsv == null || idsCsv.isBlank()) {
            return Map.of();
        }

        final List<Long> termIds;
        try {
            termIds = Arrays.stream(idsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        } catch (NumberFormatException nfe) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids 파라미터 형식이 올바르지 않습니다.");
        }

        if (termIds.isEmpty()) return Map.of();

        var rows = learningProgressRepository.findByIdAccountIdAndIdTermIdIn(accountId, termIds);

        Map<String, String> result = new LinkedHashMap<>();
        termIds.stream().distinct().forEach(id -> result.put(String.valueOf(id), "LEARNING"));
        rows.forEach(p -> result.put(String.valueOf(p.getId().getTermId()), p.getStatus().name()));

        log.info("[memo:list] done");
        return result;
    }

    @Operation(summary = "단어장 폴더 목록 + 학습 통계 조회")
    @CrossOrigin(exposedHeaders = {"X-Total-Count", "X-Page", "X-Per-Page"})
    @GetMapping("/me/wordbook/folders/stats")
    public ResponseEntity<?> getMyFoldersWithStats(
            @LoginUser Long accountId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer perPage,
            @RequestParam(required = false, defaultValue = "sortOrder, asc") String sort,
            @RequestParam(required = false) String q) {

        try {
            if (page == null) {
                var list = wordbookQueryService.getMyFoldersWithStats(accountId);
                return ResponseEntity.ok(list);
            }

            final int p = Math.max(0, page);
            final int s = Math.min(Math.max(perPage == null ? 20 : perPage, 1), 100);
            var paged = wordbookQueryService.getMyFoldersWithStatsPaged(accountId, p, s, sort, q);

            var headers = new org.springframework.http.HttpHeaders();
            headers.add("X-Total-Count", String.valueOf(paged.total()));
            headers.add("X-Page", String.valueOf(p));
            headers.add("X-Per-Page", String.valueOf(s));

            return new ResponseEntity<>(paged.items(), headers, OK);
        } catch (Exception e) {
            log.error("폴더 통계 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
