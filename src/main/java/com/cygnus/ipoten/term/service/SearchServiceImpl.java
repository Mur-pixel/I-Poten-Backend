package com.cygnus.ipoten.term.service;

import com.cygnus.ipoten.term.entity.Term;
import com.cygnus.ipoten.term.repository.TermRepository;
import com.cygnus.ipoten.term.service.request.SearchTermRequest;
import com.cygnus.ipoten.term.service.response.SearchTermResponse;
import com.cygnus.ipoten.term.support.HangulInitial;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final TermRepository termRepository;
    private final CategoryService categoryService; // 선택된 카테고리 id → 실제 검색 대상 id 집합으로 풀어주는 역할

    @Override
    public SearchTermResponse search(SearchTermRequest request) {
        log.info("search page={}, catPathIds=?, selectedCategoryId={}",
                request.getPage(), request.getCatPathIds(), request.getSelectedCategoryId());
        // 0) 카테고리 대상 id 계산
        final List<Long> targetCatIds = resolveTargetCategoryIds(request);

        // 1) 페이지/정렬 구성
        Pageable pageable = buildPageable(request, targetCatIds);

        // 2) 접두(prefix) 모드 (initial | alpha | symbol 중 1개만 세팅)
        if (request.isPrefixMode()) {
            Page<Term> page = switchPrefix(request, pageable, targetCatIds);
            return toResponse(page);
        }

        // 3) 기본 키워드 검색 / 혹은 순수 카테고리 검색
        final String q = (request.getQ() == null) ? "" : request.getQ().trim();

        // 3-1) 키워드도 없고 prefix도 없고 카테고리만 있는 경우 → 카테고리 목록만으로 조회
        if (q.isEmpty()) {
            if (targetCatIds.isEmpty()) {
                return toResponse(Page.empty(pageable)); // 아무 조건도 없으면 빈 결과
            }
            Page<Term> page = termRepository.findByTermCategoryIdIn(targetCatIds, pageable);
            return toResponse(page);
        }

        // 3-2) 키워드 검색 (+ 카테고리 필터)
        if (request.getSortKey() == SearchTermRequest.SortKey.RELEVANCE) {
            // 가중치 정렬(native) 쿼리에 카테고리 필터가 들어간 버전이 있으면 그걸 사용
            if (!targetCatIds.isEmpty()) {
                Page<Term> page = termRepository.searchByRelevanceInCategories(
                        q,
                        request.isIncludeTags(),
                        targetCatIds,
                        PageRequest.of(request.getPage(), request.getSize()) // 정렬은 native 내부 처리
                );
                return toResponse(page);
            } else {
                Page<Term> page = termRepository.searchByRelevance(
                        q,
                        request.isIncludeTags(),
                        PageRequest.of(request.getPage(), request.getSize())
                );
                return toResponse(page);
            }
        }

        // TITLE/UPDATED_AT 정렬은 JPQL LIKE + Pageable 정렬 사용
        if (!targetCatIds.isEmpty()) {
            Page<Term> page = termRepository.searchLikeInCategories(q, request.isIncludeTags(), targetCatIds, pageable);
            return toResponse(page);
        } else {
            Page<Term> page = termRepository.searchLike(q, request.isIncludeTags(), pageable);
            return toResponse(page);
        }
    }

    public List<Long> resolveTargetCategoryIds(SearchTermRequest request) {
        Long sel = request.getSelectedCategoryId();

        // fallback: selectedCategoryId 없으면 catPathIds 마지막으로 보정합니다.
        if (sel == null && request.getCatPathIds() != null && !request.getCatPathIds().isEmpty()) {
            sel = request.getCatPathIds().get(request.getCatPathIds().size() - 1);
            request.setSelectedCategoryId(sel); // 이후 로직에서도 일관되게 쓰도록
        }
        if (sel == null) return Collections.emptyList();
        return categoryService.resolveSearchTargetIds(sel);
    }

    public Pageable buildPageable(SearchTermRequest request, List<Long> targetCatIds) {
        // RELEVANCE는 네이티브 쿼리에서 자체 정렬되므로 정렬 없이 페이지네이션만
        if (request.getSortKey() == SearchTermRequest.SortKey.RELEVANCE) {
            return PageRequest.of(request.getPage(), request.getSize());
        }
        Sort sort = switch (request.getSortKey()) {
            case TITLE      -> Sort.by(request.getDirection(), "title");
            case UPDATED_AT -> Sort.by(request.getDirection(), "updatedAt");
            default         -> Sort.unsorted();
        };
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    public Page<Term> switchPrefix(SearchTermRequest request, Pageable pageable, List<Long> targetCatIds) {
        // 카테고리 필터 유무에 따라 다른 레포지토리 메서드 호출
        if (request.getInitial() != null) {
            String[] range = HangulInitial.range(request.getInitial());
            if (range == null) return Page.empty(pageable);
            if (targetCatIds.isEmpty()) {
                return termRepository.findByHangulInitialRange(range[0], range[1], pageable);
            } else {
                return termRepository.findByHangulInitialRangeInCategories(range[0], range[1], targetCatIds, pageable);
            }
        }
        if (request.getAlpha() != null) {
            if (targetCatIds.isEmpty()) {
                return termRepository.findByFirstAlpha(request.getAlpha(), pageable);
            } else {
                return termRepository.findByFirstAlphaInCategories(request.getAlpha(), targetCatIds, pageable);
            }
        }
        if (request.getSymbol() != null) {
            if (targetCatIds.isEmpty()) {
                return termRepository.findByFirstSymbol(request.getSymbol(), pageable);
            } else {
                return termRepository.findByFirstSymbolInCategories(request.getSymbol(), targetCatIds, pageable);
            }
        }
        return Page.empty(pageable);
    }

    public SearchTermResponse toResponse(Page<Term> page) {
        var items = page.getContent().stream()
                .map(t -> SearchTermResponse.TermSummary.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .build())
                .collect(Collectors.toList());

        return SearchTermResponse.builder()
                .total(page.getTotalElements())
                .items(items)
                .build();
    }
}
