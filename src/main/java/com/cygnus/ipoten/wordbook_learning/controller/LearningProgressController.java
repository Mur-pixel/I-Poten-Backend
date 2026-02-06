package com.cygnus.ipoten.wordbook_learning.controller;

import com.cygnus.ipoten.wordbook_learning.service.LearningProgressService;
import com.cygnus.ipoten.wordbook_learning.controller.request_form.UpdateLearningProgressRequestForm;
import com.cygnus.ipoten.wordbook_learning.controller.response_form.UpdateLearningProgressResponseForm;
import com.cygnus.ipoten.wordbook_learning.repository.LearningProgressRepository;
import com.cygnus.ipoten.wordbook.service.WordbookQueryService;
import com.cygnus.ipoten.wordbook_learning.service.request.UpdateLearningProgressRequest;
import com.cygnus.ipoten.wordbook_learning.service.response.UpdateLearningProgressResponse;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
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

    private final RedisCacheService redisCacheService;
    private final LearningProgressService learningProgressService;
    private final WordbookQueryService wordbookQueryService;
    private final LearningProgressRepository learningProgressRepository;

    @Operation(
            summary = "단어 학습 상태 변경",
            description = "특정 용어에 대한 자신의 학습 상태(LEARNING / DONE 등)를 변경합니다."
    )
    @PatchMapping("/me/terms/{termId}/memorization")
    public UpdateLearningProgressResponseForm updateMemorizationByTermId(
            @CookieValue(name = "userToken", required = false) String userToken,
            @PathVariable Long termId,
            @RequestBody @Valid UpdateLearningProgressRequestForm requestForm
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[memo:update:byTerm] 인증 실패");
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }
        UpdateLearningProgressRequest request = requestForm.toUpdateMemorizationRequest(accountId, termId);
        UpdateLearningProgressResponse response = learningProgressService.updateMemorization(request);
        log.info("[memo:update:byTerm] done");
        return UpdateLearningProgressResponseForm.from(response);
    }

    @Operation(
            summary = "여러 단어의 학습 상태 일괄 조회",
            description = "ids=1,2,3 형태의 CSV로 용어 ID 목록을 전달하면, 각 용어의 학습 상태를 반환합니다. 기본값은 LEARNING입니다."
    )
    @GetMapping("/me/terms/memorization")
    public Map<String, String> getMemorizationStatuses(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestParam(name = "ids") String idsCsv
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "로그인이 필요합니다.");
        }

        // 빈/공백 방어
        if (idsCsv == null || idsCsv.isBlank()) {
            return Map.of();
        }

        // CSV -> List<Long>
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

        // 기본값 LEARNING으로 채워 두고, 조회된 건 덮어쓰기
        Map<String, String> result = new LinkedHashMap<>();
        termIds.stream().distinct().forEach(id -> result.put(String.valueOf(id), "LEARNING"));
        rows.forEach(p -> result.put(String.valueOf(p.getId().getTermId()), p.getStatus().name()));

        log.info("[memo:list] done");
        return result;
    }

    @Operation(
            summary = "단어장 폴더 목록 + 학습 통계 조회",
            description = """
                    폴더별 단어 수, 학습 완료 개수 등을 포함한 통계 정보를 조회합니다.
                    - page 파라미터가 없으면: 전체 목록을 배열로 반환
                    - page가 있으면: 페이지네이션 + X-Total-Count / X-Page / X-Per-Page 헤더 포함
                    """
    )
    @CrossOrigin(exposedHeaders = {"X-Total-Count","X-Page","X-Per-Page"})
    @GetMapping("/me/wordbook/folders/stats")
    public ResponseEntity<?> getMyFoldersWithStats(
            @CookieValue(name = "userToken", required = false) String userToken,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer perPage,
            @RequestParam(required = false, defaultValue = "sortOrder, asc") String sort,
            @RequestParam(required = false) String q
    ) {
        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("인증 실패: 계정 식별 불가");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 1) 페이지 파라미터가 없는 경우: 기존 동작(전체 배열)
            if (page == null) {
                var list = wordbookQueryService.getMyFoldersWithStats(accountId);
                return ResponseEntity.ok(list);
            }

            // 2) 페이지 파라미터 있는 경우: 페이징 + 헤더 메타데이터
            final int p = Math.max(0, page);
            final int s = Math.min(Math.max(perPage == null ? 20 : perPage, 1), 100);

            var paged = wordbookQueryService.getMyFoldersWithStatsPaged(accountId, p, s, sort, q);

            var headers = new org.springframework.http.HttpHeaders();
            headers.add("X-Total-Count", String.valueOf(paged.total())); // 총 폴더 수(그룹 행 수)
            headers.add("X-Page", String.valueOf(p));
            headers.add("X-Per-Page", String.valueOf(s));

            return new ResponseEntity<>(paged.items(), headers, OK);
        } catch (Exception e) {
            log.error("폴더 통계 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 공통: userToken 쿠키에서 계정 ID를 조회한다.
     * - 토큰이 없거나 공백이면 null
     * - Redis에 없거나 TTL 만료된 경우도 null
     * → null이면 컨트롤러에서 UNAUTHORIZED 처리
     */
    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) return null;
        return redisCacheService.getValueByKey(userToken, Long.class); // TTL 만료/무효면 null
    }
}
