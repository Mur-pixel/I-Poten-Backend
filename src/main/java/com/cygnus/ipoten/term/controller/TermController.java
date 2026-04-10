package com.cygnus.ipoten.term.controller;

import com.cygnus.ipoten.common.annotation.LoginUser;
import com.cygnus.ipoten.common.annotation.PublicEndpoint;
import com.cygnus.ipoten.redis_cache.RedisCacheService;
import com.cygnus.ipoten.term.controller.request_form.CreateTermRequestForm;
import com.cygnus.ipoten.term.controller.request_form.ListTermRequestForm;
import com.cygnus.ipoten.term.controller.request_form.SearchRequestForm;
import com.cygnus.ipoten.term.controller.request_form.UpdateTermRequestForm;
import com.cygnus.ipoten.term.controller.response_form.CreateTermResponseForm;
import com.cygnus.ipoten.term.controller.response_form.ListTermResponseForm;
import com.cygnus.ipoten.term.controller.response_form.SearchTermResponseForm;
import com.cygnus.ipoten.term.controller.response_form.UpdateTermResponseForm;
import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermTagRepository;
import com.cygnus.ipoten.term.service.SearchService;
import com.cygnus.ipoten.term.service.TagTextReader;
import com.cygnus.ipoten.term.service.TermService;
import com.cygnus.ipoten.term.service.request.ListTermRequest;
import com.cygnus.ipoten.term.service.response.CreateTermResponse;
import com.cygnus.ipoten.term.service.response.ListTermResponse;
import com.cygnus.ipoten.term.service.response.UpdateTermResponse;
import com.cygnus.ipoten.term.support.TagTextParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/terms")
@Tag(name = "Term", description = "용어(IT 개념) CRUD 및 검색 API")
public class TermController {

    private final TermService termService;
    private final SearchService searchService;
    private final TermTagRepository termTagRepository;
    private final TagTextReader tagTextReader;
    private final RedisCacheService redisCacheService;  // search()의 optional auth에 사용

    @Operation(summary = "용어 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "용어 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateTermResponseForm.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public ResponseEntity<CreateTermResponseForm> createTerm(
            @Valid @RequestBody CreateTermRequestForm createTermRequestForm,
            @LoginUser Long accountId) {

        log.debug("용어 생성 요청 - 제목: {}", createTermRequestForm.getTitle());
        try {
            CreateTermResponse response = termService.register(createTermRequestForm.toCreateTermRequest());
            log.info("용어 생성 완료 - 용어 ID: {}", response.getTermId());
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("용어 생성 실패 - 제목: {}", createTermRequestForm.getTitle(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "용어 수정")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "용어 수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateTermResponseForm.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/{termId}")
    public ResponseEntity<UpdateTermResponseForm> updateTerm(
            @Parameter(description = "수정할 용어 ID", example = "1")
            @PathVariable Long termId,
            @Valid @RequestBody UpdateTermRequestForm updateTermRequestForm,
            @LoginUser Long accountId) {

        log.debug("용어 수정 요청 - 용어 ID: {}", termId);
        try {
            UpdateTermResponse response = termService.updateTerm(updateTermRequestForm.toUpdateTermRequest(termId));
            log.info("용어 수정 완료 - 용어 ID: {}", termId);
            return ResponseEntity.ok(UpdateTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("용어 수정 실패 - 용어 ID: {}", termId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "용어 삭제")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "용어 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{termId}")
    public ResponseEntity<Void> deleteTerm(
            @Parameter(description = "삭제할 용어 ID", example = "1")
            @PathVariable Long termId,
            @LoginUser Long accountId) {

        log.debug("용어 삭제 요청 - 용어 ID: {}", termId);
        try {
            termService.deleteTerm(termId);
            log.info("용어 삭제 완료 - 용어 ID: {}", termId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("용어 삭제 실패 - 용어 ID: {}", termId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PublicEndpoint
    @Operation(summary = "용어 목록 조회")
    @GetMapping
    public ResponseEntity<ListTermResponseForm> termList(
            @Valid @ModelAttribute ListTermRequestForm requestForm) {
        try {
            ListTermRequest request = requestForm.toListTermRequest();
            ListTermResponse response = termService.list(request);
            return ResponseEntity.ok(ListTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("용어 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // optional auth — 비로그인도 검색 가능, 로그인 시 사용자 맞춤 결과
    @PublicEndpoint
    @Operation(summary = "용어 검색")
    @GetMapping("/search")
    public ResponseEntity<SearchTermResponseForm> search(
            @Valid @ModelAttribute SearchRequestForm requestForm,
            @CookieValue(name = "userToken", required = false) String userToken,
            @CookieValue(name = ANON_COOKIE_NAME, required = false) String anonId,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        try {
            Long accountId = resolveAccountIdOptional(userToken);
            String ensuredAnonId = resolveOrIssueAnonId(anonId, httpServletRequest, httpServletResponse);

            var request = requestForm.toRequest();
            request.setActorKey(buildActorKey(accountId, ensuredAnonId));

            var response = searchService.search(request);
            return ResponseEntity.ok(SearchTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("용어 검색 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PublicEndpoint
    @Operation(summary = "단일 용어 태그 조회")
    @GetMapping("/{termId}/tags")
    public ResponseEntity<List<String>> tags(
            @Parameter(description = "태그를 조회할 용어 ID", example = "1")
            @PathVariable Long termId) {
        try {
            List<String> names = termTagRepository.findAllNamesByTermId(termId);
            if (!names.isEmpty()) {
                return ResponseEntity.ok(names);
            }
            String raw = tagTextReader.readRaw(termId).orElse(null);
            List<String> parsed = TagTextParser.parse(raw);
            return ResponseEntity.ok(parsed);
        } catch (Exception e) {
            log.error("[tags] 조회 실패 - 용어 ID: {}", termId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PublicEndpoint
    @Operation(summary = "여러 용어 태그 배치 조회")
    @GetMapping("/tags")
    public ResponseEntity<Map<Long, List<String>>> tagsByIds(
            @RequestParam("ids") List<Long> ids) {
        try {
            var rows = termTagRepository.findTermIdAndTagNameByTermIdIn(ids);
            Map<Long, List<String>> map = new LinkedHashMap<>();
            for (var r : rows) {
                map.computeIfAbsent(r.getTermId(), k -> new ArrayList<>()).add(r.getTagName());
            }

            for (Long id : ids) {
                String raw = tagTextReader.readRaw(id).orElse(null);
                List<String> parsed = TagTextParser.parse(raw);
                if (!parsed.isEmpty()) {
                    if (!map.containsKey(id) || map.get(id).isEmpty()) {
                        map.put(id, parsed);
                    } else {
                        Set<String> merged = new LinkedHashSet<>(map.get(id));
                        merged.addAll(parsed);
                        map.put(id, new ArrayList<>(merged));
                    }
                }
            }

            map.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().sorted().toList());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error("[tagsByIds] 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PublicEndpoint
    @Operation(summary = "태그 기준 용어 검색")
    @GetMapping("/search/by-tag")
    public ResponseEntity<ListTermResponseForm> searchByTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            ListTermResponse response = termService.searchByTag(tag, page, size);
            List<Long> ids = response.getTermList().stream().map(Term::getId).toList();
            Map<Long, List<String>> tagMap = new HashMap<>();
            if (!ids.isEmpty()) {
                var rows = termTagRepository.findTermIdAndTagNameByTermIdIn(ids);
                for (var r : rows) {
                    tagMap.computeIfAbsent(r.getTermId(), k -> new ArrayList<>()).add(r.getTagName());
                }
                tagMap.replaceAll((k, v) -> v.stream().distinct().sorted().toList());
            }
            response.setTagsByTermId(tagMap);
            return ResponseEntity.ok(ListTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("[searchByTag] 조회 실패 - 태그: {}", tag, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ---- 검색 전용 optional auth (비로그인 허용) ----

    private Long resolveAccountIdOptional(String userToken) {
        if (userToken == null || userToken.isBlank()) return null;
        return redisCacheService.getValueByKey(userToken, Long.class);
    }

    private static final String ANON_COOKIE_NAME = "anonId";
    private static final Duration ANON_COOKIE_MAX_AGE = Duration.ofDays(365);

    private String resolveOrIssueAnonId(String anonId, HttpServletRequest request, HttpServletResponse response) {
        if (anonId != null && !anonId.isBlank()) return anonId;

        String newAnonId = UUID.randomUUID().toString().replaceAll("-", "");

        boolean secure = request.isSecure();
        ResponseCookie cookie = ResponseCookie.from(ANON_COOKIE_NAME, newAnonId)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(ANON_COOKIE_MAX_AGE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return newAnonId;
    }

    private String buildActorKey(Long accountId, String anonId) {
        if (accountId != null) return "A_" + accountId;
        return "N_" + anonId;
    }
}
