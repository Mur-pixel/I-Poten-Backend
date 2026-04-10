package com.cygnus.ipoten.term.service;

import com.cygnus.ipoten.term_category.entity.TermCategory;

import java.util.List;

public interface CategoryService {
    List<TermCategory> findCategories(Integer depth, Long parentId);
    /**
     * 검색 대상 카테고리 ID 집합을 계산한다.
     * - 언어 중심(depth=1) 선택 시: 해당 id 자체가 검색 대상
     * - 직무/기타에서 대분류(depth=0) 선택 시: 그 하위 모든 소분류(depth=2) id
     * - 직무/기타에서 중분류(depth=1) 선택 시: 그 하위 소분류(depth=2) id
     * - 소분류(depth=2) 선택 시: 해당 id 단독
     */
    List<Long> resolveSearchTargetIds(Long selectedCategoryId);
}
