package com.cygnus.ipoten.term.repository;

import com.cygnus.ipoten.term.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findById(Long categoryId);
    Optional<Category> findFirstByNameAndDepth(String name, Integer depth);

    // depth만으로 루트(대분류) 조회
    List<Category> findByDepthOrderBySortOrder(Integer depth);

    // parentId로 하위(중/소분류) 조회
    List<Category> findByDepthAndParentIdOrderBySortOrder(Integer depth, Long parentId);

    boolean existsById(Long id);

    // 하위 탐색용
    List<Category> findAllByParent_Id(Long parentId);
    List<Category> findAllByParent_IdIn(Collection<Long> parentIds);
}
