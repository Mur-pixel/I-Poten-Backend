package com.cygnus.ipoten.term.service;

import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term_category.repository.TermCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final TermCategoryRepository termCategoryRepository;

    @Override
    public List<TermCategory> findCategories(Integer depth, Long parentId) {

        if (depth == null || parentId == null) {
            // 기본값: 최상위 카테고리
            return termCategoryRepository.findByDepthOrderBySortOrder(0);
        }

        if (depth != null && parentId == null) {
            return termCategoryRepository.findByDepthOrderBySortOrder(depth);
        }

        if (depth != null && parentId != null) {
            return termCategoryRepository.findByDepthAndParentIdOrderBySortOrder(depth, parentId);
        }

        // 잘못된 요청 : 빈 배열 반환
        return List.of();
    }

    @Override
    public List<Long> resolveSearchTargetIds(Long selectedCategoryId) {
        if (selectedCategoryId == null) return List.of();

        TermCategory sel = termCategoryRepository.findById(selectedCategoryId)
                .orElse(null);
        if (sel == null) return List.of();

        // 언어 중심: depth=1에서 바로 Term가 매핑될 수 있음 → 본인 id만
        if ("언어 중심".equals(sel.getType()) && sel.getDepth() == 1) {
            return List.of(sel.getId());
        }

        // 소분류 → 본인 id만
        if (sel.getDepth() == 2) return List.of(sel.getId());

        // 중분류(직무/기타) → 하위 소분류 모두
        if (sel.getDepth() == 1) {
            List<TermCategory> children = termCategoryRepository.findAllByParent_Id(sel.getId());
            List<Long> out = new ArrayList<>();
            for (TermCategory c : children) {
                if (c.getDepth() == 2) out.add(c.getId());
            }
            return out;
        }

        // 대분류(0) → (중분류들) → (소분류들) 전체 수집
        if (sel.getDepth() == 0) {
            List<TermCategory> mids = termCategoryRepository.findAllByParent_Id(sel.getId());
            if (mids.isEmpty()) return List.of();
            List<Long> midIds = mids.stream().map(TermCategory::getId).toList();
            List<TermCategory> leaves = termCategoryRepository.findAllByParent_IdIn(midIds);
            return leaves.stream()
                    .filter(c -> c.getDepth() == 2)
                    .map(TermCategory::getId)
                    .toList();
        }

        return List.of(sel.getId());
    }
}
