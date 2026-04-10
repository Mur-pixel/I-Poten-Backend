package com.cygnus.ipoten.term_category.repository;

import com.cygnus.ipoten.term_category.entity.TermCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TermCategoryRepository extends JpaRepository<TermCategory, Long> {
    Optional<TermCategory> findById(Long categoryId);
    Optional<TermCategory> findFirstByNameAndDepth(String name, Integer depth);

    // depth만으로 루트(대분류) 조회
    List<TermCategory> findByDepthOrderBySortOrder(Integer depth);

    // parentId로 하위(중/소분류) 조회
    List<TermCategory> findByDepthAndParentIdOrderBySortOrder(Integer depth, Long parentId);

    boolean existsById(Long id);

    // 하위 탐색용
    List<TermCategory> findAllByParent_Id(Long parentId);
    List<TermCategory> findAllByParent_IdIn(Collection<Long> parentIds);
}
