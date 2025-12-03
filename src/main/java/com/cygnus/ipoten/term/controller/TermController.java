package com.cygnus.ipoten.term.controller;

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final RedisCacheService redisCacheService;

    /** 공통: 쿠키에서 토큰 추출 후 Redis에서 accountId 조회(없으면 null) */
    private Long resolveAccountId(String userToken) {
        if (userToken == null || userToken.isBlank()) {
            return null;
        }
        return redisCacheService.getValueByKey(userToken, Long.class); // TTL 만료/무효면 null
    }

    // 용어 등록 (제목, 설명, 태그, 카테고리)
    @Operation(
            summary = "용어 등록",
            description = "제목, 설명, 태그, 카테고리 정보를 입력하여 새로운 IT 용어를 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "용어 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateTermResponseForm.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패 (userToken 없음 또는 만료)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping
    public ResponseEntity<CreateTermResponseForm> createTerm(
            @Valid @RequestBody CreateTermRequestForm createTermRequestForm,
            @Parameter(description = "로그인 후 발급되는 사용자 토큰 (쿠키)", required = false)
            @CookieValue(name = "userToken", required = false) String userToken) {

        log.debug("용어 생성 요청 - 제목: {}, 카테고리 ID: {}", createTermRequestForm.getTitle(), createTermRequestForm.getCategoryId());

        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[createTerm] 인증 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CreateTermResponse response = termService.register(createTermRequestForm.toCreateTermRequest());
            log.info("용어 생성 완료 - 용어 ID: {}", response.getTermId());
            log.debug("용어 생성 상세 - 제목: {}", response.getTitle());
            return ResponseEntity.status(HttpStatus.CREATED).body(CreateTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("용어 생성 실패 - 제목: {}", createTermRequestForm.getTitle(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // PUT /api/terms/{termId} — 용어 정보 수정
    @Operation(
            summary = "용어 수정",
            description = "기존에 등록된 용어의 제목, 설명, 태그, 카테고리 정보를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "용어 수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateTermResponseForm.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패 (userToken 없음 또는 만료)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PutMapping("/{termId}")
    public ResponseEntity<UpdateTermResponseForm> updateTerm(
            @Parameter(description = "수정할 용어 ID", example = "1")
            @PathVariable Long termId,
            @Valid @RequestBody UpdateTermRequestForm updateTermRequestForm,
            @Parameter(description = "로그인 후 발급되는 사용자 토큰 (쿠키)", required = false)
            @CookieValue(name = "userToken", required = false) String userToken) {

        log.debug("용어 수정 요청 - 용어 ID: {}, 제목: {}", termId, updateTermRequestForm.getTitle());

        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[updateTerm] 인증 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            UpdateTermResponse response = termService.updateTerm(updateTermRequestForm.toUpdateTermRequest(termId));
            log.info("용어 수정 완료 - 용어 ID: {}", termId);
            log.debug("용어 수정 상세 - 제목: {}", response.getTitle());
            return ResponseEntity.ok(UpdateTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("용어 수정 실패 - 용어 ID: {}", termId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DELETE /api/terms/{termId} — 용어 삭제
    @Operation(
            summary = "용어 삭제",
            description = "특정 용어를 삭제합니다. 관련 퀴즈/단어장 등 영향도는 별도 정책에 따릅니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "용어 삭제 성공 (본문 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (userToken 없음 또는 만료)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping("/{termId}")
    public ResponseEntity<Void> deleteTerm(
            @Parameter(description = "삭제할 용어 ID", example = "1")
            @PathVariable Long termId,
            @Parameter(description = "로그인 후 발급되는 사용자 토큰 (쿠키)", required = false)
            @CookieValue(name = "userToken", required = false) String userToken) {

        log.debug("용어 삭제 요청 - 용어 ID: {}", termId);

        Long accountId = resolveAccountId(userToken);
        if (accountId == null) {
            log.warn("[deleteTerm] 인증 실패 - 용어 ID: {}", termId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            termService.deleteTerm(termId);
            log.info("용어 삭제 완료 - 용어 ID: {}", termId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("용어 삭제 실패 - 용어 ID: {}", termId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 모든 용어를 페이지 단위로 확인하기
    @Operation(
            summary = "용어 목록 조회",
            description = "페이지/크기 기준으로 전체 용어 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ListTermResponseForm.class))
            ),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public ResponseEntity<ListTermResponseForm> termList(
            @Valid @ModelAttribute ListTermRequestForm requestForm) {
        log.debug("용어 목록 조회 요청 - 페이지: {}, 크기: {}", requestForm.getPage(), requestForm.getPerPage());
        try {
            ListTermRequest request = requestForm.toListTermRequest();
            ListTermResponse response = termService.list(request);
            log.debug("용어 목록 조회 완료 - 총 개수: {}, 현재 페이지 항목 수: {}", response.getTotalItems(), response.getTermList().size());
            return ResponseEntity.ok(ListTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("용어 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // '가장 일치하는 제목' 기준 검색
    @Operation(
            summary = "용어 검색",
            description = "검색어(q)를 기준으로 가장 일치하는 제목/설명을 가진 용어를 검색합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = SearchTermResponseForm.class))
            ),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<SearchTermResponseForm> search(
            @Valid @ModelAttribute SearchRequestForm requestForm) {
        log.debug("용어 검색 요청 - 페이지: {}, 크기: {}", requestForm.getPage(), requestForm.getSize());
        try {
            var response = searchService.search(requestForm.toRequest());
            log.debug("용어 검색 완료 - 검색 결과 수: {}", response.getItems().size());
            return ResponseEntity.ok(SearchTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("용어 검색 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 단건 태그 조회
    @Operation(
            summary = "단일 용어 태그 조회",
            description = "특정 용어 ID에 연결된 태그 목록을 조회합니다. 정규화 테이블 → 폴백 파싱 순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "태그 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class)))
            ),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{termId}/tags")
    public ResponseEntity<List<String>> tags(
            @Parameter(description = "태그를 조회할 용어 ID", example = "1")
            @PathVariable Long termId) {
        log.debug("용어 태그 조회 요청 - 용어 ID: {}", termId);
        try {
            // 1) 정규화 테이블 조인
            log.debug("정규화 테이블에서 태그 조회 중 - 용어 ID: {}", termId);
            List<String> names = termTagRepository.findAllNamesByTermId(termId);
            if (!names.isEmpty()) {
                log.debug("정규화 테이블에서 태그 발견 - 용어 ID: {}, 태그 수: {}", termId, names.size());
                return ResponseEntity.ok(names);
            }

            // 2) 폴백: term 테이블의 태그 문자열에서 파싱
            log.debug("폴백 텍스트 파싱 시도 - 용어 ID: {}", termId);
            String raw = tagTextReader.readRaw(termId).orElse(null);
            List<String> parsed = TagTextParser.parse(raw);
            log.debug("태그 조회 완료 - 용어 ID: {}, 정규화 테이블: {}, 파싱된 태그: {}", termId, names.size(), parsed.size());
            return ResponseEntity.ok(parsed);
        } catch (Exception e) {
            log.error("[tags] 조회 실패 - 용어 ID: {}", termId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 배치 태그 조회: /api/terms/tags?ids=1&ids=2...
    @Operation(
            summary = "여러 용어 태그 배치 조회",
            description = "`ids` 쿼리 파라미터로 전달된 여러 용어 ID에 대해 태그 목록을 한 번에 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "배치 태그 조회 성공",
                    content = @Content(
                            schema = @Schema(
                                    description = "termId → 태그 목록 매핑",
                                    implementation = Map.class
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/tags")
    public ResponseEntity<Map<Long, List<String>>> tagsByIds(
            @Parameter(
                    description = "태그를 조회할 용어 ID 목록. 예: ids=1&ids=2&ids=3",
                    required = true
            )
            @RequestParam("ids") List<Long> ids) {
        log.info("배치 태그 조회 요청 - 총 {}개 ID", ids.size());
        try {
            // 1) 정규화 테이블에서 최대한 수집
            var rows = termTagRepository.findTermIdAndTagNameByTermIdIn(ids);
            Map<Long, List<String>> map = new LinkedHashMap<>();
            for (var r : rows) {
                map.computeIfAbsent(r.getTermId(), k -> new ArrayList<>()).add(r.getTagName());
            }
            log.debug("정규화 테이블 조회 완료 - {}개 용어", map.size());

            // 2) 비어있는 항목은 폴백으로 텍스트 파싱(및 병합)
            for (Long id : ids) {
                if (!map.containsKey(id) || map.get(id).isEmpty()) {
                    String raw = tagTextReader.readRaw(id).orElse(null);
                    List<String> parsed = TagTextParser.parse(raw);
                    if (!parsed.isEmpty()) map.put(id, parsed);
                } else {
                    String raw = tagTextReader.readRaw(id).orElse(null);
                    List<String> parsed = TagTextParser.parse(raw);
                    if (!parsed.isEmpty()) {
                        Set<String> merged = new LinkedHashSet<>(map.get(id));
                        merged.addAll(parsed);
                        map.put(id, new ArrayList<>(merged));
                    }
                }
            }

            // 3) 정렬 + 중복 제거
            map.replaceAll((k, v) -> v.stream().filter(Objects::nonNull).distinct().sorted().toList());
            log.info("배치 태그 조회 완료 - 요청 {}개, 결과 {}개", ids.size(), map.size());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error("[tagsByIds] 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 연관 키워드(해시태그) 클릭 시 같은 태그의 용어만 조회
    @Operation(
            summary = "태그 기준 용어 검색",
            description = "특정 태그를 기준으로 해당 태그가 포함된 용어 목록을 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "태그 검색 성공",
                    content = @Content(schema = @Schema(implementation = ListTermResponseForm.class))
            ),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/search/by-tag")
    public ResponseEntity<ListTermResponseForm> searchByTag(
            @Parameter(description = "검색할 태그 문자열", example = "#Java")
            @RequestParam String tag,
            @Parameter(description = "페이지 번호(1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지당 조회할 개수", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        log.debug("태그별 용어 검색 요청 - 태그: {}, 페이지: {}, 크기: {}", tag, page, size);
        try {
            ListTermResponse response = termService.searchByTag(tag, page, size);

            // termId들 뽑아서 태그 조회
            List<Long> ids = response.getTermList().stream().map(Term::getId).toList();
            log.debug("검색된 용어들의 태그 조회 - 용어 수: {}", ids.size());

            Map<Long, List<String>> tagMap = new HashMap<>();
            if (!ids.isEmpty()) {
                var rows = termTagRepository.findTermIdAndTagNameByTermIdIn(ids);
                for (var r : rows) {
                    tagMap.computeIfAbsent(r.getTermId(), k -> new ArrayList<>()).add(r.getTagName());
                }
                // 중복 제거 + 정렬
                tagMap.replaceAll((k, v) -> v.stream().distinct().sorted().toList());
            }
            response.setTagsByTermId(tagMap);

            log.debug("태그별 용어 검색 완료 - 태그: {}, 결과: {}개", tag, response.getTermList().size());
            return ResponseEntity.ok(ListTermResponseForm.from(response));
        } catch (Exception e) {
            log.error("[searchByTag] 조회 실패 - 태그: {}", tag, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}