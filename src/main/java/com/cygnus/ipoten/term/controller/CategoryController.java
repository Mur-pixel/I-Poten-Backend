package com.cygnus.ipoten.term.controller;

import com.cygnus.ipoten.term.controller.dto.CategoryDto;
import com.cygnus.ipoten.term.entity.Category;
import com.cygnus.ipoten.term.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<CategoryDto> list(
            @RequestParam(name = "depth") Integer depth,
            @RequestParam(name = "parentId", required = false) Long parentIdCamel,
            @RequestParam(name = "parent_id", required = false) Long parentIdSnake
    ) {
        Long parentId = parentIdCamel != null ? parentIdCamel : parentIdSnake;

        if (depth == null) {
            log.warn("categories: depth is null -> return []");
            return List.of();
        }

        if (depth == 0) {
            var list = categoryRepository.findByDepthOrderBySortOrder(0);
            return list.stream().map(CategoryDto::from).toList();
        }

        if (parentId == null) {
            log.debug("categories: depth={} but parentId is null -> []", depth);
            return List.of();
        }

        if (!categoryRepository.existsById(parentId)) {
            log.debug("categories: invalid parentId={} -> []", parentId);
            return List.of();
        }

        List<Category> list = categoryRepository.findByDepthAndParentIdOrderBySortOrder(depth, parentId);
        return list.stream().map(CategoryDto::from).toList();
    }
}