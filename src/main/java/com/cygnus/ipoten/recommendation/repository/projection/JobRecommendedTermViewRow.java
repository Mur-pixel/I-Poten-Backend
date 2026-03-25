package com.cygnus.ipoten.recommendation.repository.projection;

public record JobRecommendedTermViewRow(
        Long termId,
        String title,
        String description,
        Integer rankNo,
        Long categoryId,
        String categoryName
) {
}
