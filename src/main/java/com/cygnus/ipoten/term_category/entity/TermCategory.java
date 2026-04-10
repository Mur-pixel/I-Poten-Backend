package com.cygnus.ipoten.term_category.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "term_category",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_category_parent_name",
                columnNames = {"parent_id", "name"}
        )
)
public class TermCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;        // 대분류(직무 중심, 언어 중심, 기타)

    @Column(name = "group_name", nullable = false)
    private String groupName;   // 중분류(Frontend, Backend, Database 등)

    @Column(nullable = false)
    private String name;        // 소분류

    @Column(nullable = false)
    private Integer depth;      // 0 : 대분류, 1 : 중분류, 2 : 소분류

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;  // UI에서 카테고리를 정렬할 때 쓰는 필드

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TermCategory parent;    // 상위 카테고리(nullable)

    @Builder
    private TermCategory(String type, String groupName, String name, Integer depth, Integer sortOrder, TermCategory parent) {
        this.type = type;
        this.groupName = groupName;
        this.name = name;
        this.depth = depth;
        this.sortOrder = sortOrder;
        this.parent = parent;
    }
}
