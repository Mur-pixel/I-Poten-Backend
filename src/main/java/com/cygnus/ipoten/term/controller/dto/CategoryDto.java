package com.cygnus.ipoten.term.controller.dto;

import com.cygnus.ipoten.term.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CategoryDto {
    private Long id;
    private String type;
    private String group_name;   // 프론트가 snake/camel 둘 다 인식하지만 통일감을 주자
    private String name;
    private Integer depth;
    private Integer sort_order;
    private Long parent_id;

    public static CategoryDto from(Category c) {
        return CategoryDto.builder()
                .id(c.getId())
                .type(c.getType())
                .group_name(c.getGroupName())
                .name(c.getName())
                .depth(c.getDepth())
                .sort_order(c.getSortOrder())
                .parent_id(c.getParent() != null ? c.getParent().getId() : null)
                .build();
    }
}