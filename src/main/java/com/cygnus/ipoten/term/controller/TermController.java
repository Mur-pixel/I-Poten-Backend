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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/terms")
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
    @PostMapping
    public ResponseEntity<CreateTermResponseForm> createTerm(
            @Valid @RequestBody CreateTermRequestForm createTermRequestForm,
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
    @PutMapping("/{termId}")
    public ResponseEntity<UpdateTermResponseForm> updateTerm(
            @PathVariable Long termId,
            @Valid @RequestBody UpdateTermRequestForm updateTermRequestForm,
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
    @DeleteMapping("/{termId}")
    public ResponseEntity<Void> deleteTerm(
            @PathVariable Long termId,
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
    @GetMapping
    public ResponseEntity<ListTermResponseForm> termList(@Valid @ModelAttribute ListTermRequestForm requestForm) {
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
    @GetMapping("/search")
    public ResponseEntity<SearchTermResponseForm> search(@Valid @ModelAttribute SearchRequestForm requestForm) {
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
    @GetMapping("/{termId}/tags")
    public ResponseEntity<List<String>> tags(@PathVariable Long termId) {
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
    @GetMapping("/tags")
    public ResponseEntity<Map<Long, List<String>>> tagsByIds(@RequestParam("ids") List<Long> ids) {
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
    @GetMapping("/search/by-tag")
    public ResponseEntity<ListTermResponseForm> searchByTag(
            @RequestParam String tag,
            @RequestParam(defaultValue = "1") int page,
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
