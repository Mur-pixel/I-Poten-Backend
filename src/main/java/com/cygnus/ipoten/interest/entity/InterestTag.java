package com.cygnus.ipoten.interest.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "interest_tag",
        indexes = {
                @Index(name = "idx_interest_tag_interest_id", columnList = "interest_id"),
                @Index(name = "idx_interest_tag_active_sort", columnList = "is_active, sort_order")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interest_tag_interest_name",
                        columnNames = {"interest_id", "name"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterestTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Interest interest;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sortOrder", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InterestTag(
            Interest interest,
            String name,
            Integer sortOrder,
            boolean active
    ) {
        this.interest = interest;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = active;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(Interest interest, String name, Integer sortOrder, boolean active) {
        this.interest = interest;
        this.name = name;
        this.sortOrder = sortOrder;
        this.active = active;
        this.updatedAt = Instant.now();
    }
}
