package com.cygnus.ipoten.term_category.controller;

import com.cygnus.ipoten.term_category.controller.response_form.CategoryResponseForm;
import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term_category.repository.TermCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Swagger / OpenAPI 관련 import
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
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "카테고리 트리 조회 API")
public class TermCategoryController {

    private final TermCategoryRepository termCategoryRepository;

    @Operation(
            summary = "카테고리 목록 조회",
            description = """
                    depth와 parentId를 기준으로 카테고리 목록을 조회합니다.
                    - depth = 0: 루트 카테고리 목록
                    - depth > 0: parentId 하위의 자식 카테고리 목록
                    parentId는 parentId 또는 parent_id 둘 중 아무 쿼리 파라미터나 사용 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResponseForm.class))
                    )
            )
    })
    @GetMapping
    public List<CategoryResponseForm> list(
            @Parameter(
                    description = "카테고리 깊이 (0: 루트)",
                    example = "0",
                    required = true
            )
            @RequestParam(name = "depth") Integer depth,

            @Parameter(
                    description = "부모 카테고리 ID (camelCase)",
                    example = "1"
            )
            @RequestParam(name = "parentId", required = false) Long parentIdCamel,

            @Parameter(
                    description = "부모 카테고리 ID (snake_case)",
                    example = "1"
            )
            @RequestParam(name = "parent_id", required = false) Long parentIdSnake
    ) {
        Long parentId = parentIdCamel != null ? parentIdCamel : parentIdSnake;

        if (depth == null) {
            log.warn("categories: depth is null -> return []");
            return List.of();
        }

        if (depth == 0) {
            var list = termCategoryRepository.findByDepthOrderBySortOrder(0);
            return list.stream().map(CategoryResponseForm::from).toList();
        }

        if (parentId == null) {
            log.debug("categories: depth={} but parentId is null -> []", depth);
            return List.of();
        }

        if (!termCategoryRepository.existsById(parentId)) {
            log.debug("categories: invalid parentId={} -> []", parentId);
            return List.of();
        }

        List<TermCategory> list = termCategoryRepository.findByDepthAndParentIdOrderBySortOrder(depth, parentId);
        return list.stream().map(CategoryResponseForm::from).toList();
    }
}