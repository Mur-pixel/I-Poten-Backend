package com.cygnus.iptn.recommendation.repository.projection;

public record JobRecommendedTermViewRow(
        Long termId,
        String title,
        String description,
        Integer rankNo,
        Long categoryId,
        String categoryName
) {
}
