package com.cygnus.ipoten.term_category.controller.response_form;

import com.cygnus.ipoten.term_category.entity.TermCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CategoryResponseForm {
    private Long id;
    private String type;
    private String group_name;
    private String name;
    private Integer depth;
    private Integer sort_order;
    private Long parent_id;

    public static CategoryResponseForm from(TermCategory c) {
        return CategoryResponseForm.builder()
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