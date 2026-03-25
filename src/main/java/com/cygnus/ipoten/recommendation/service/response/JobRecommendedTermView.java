package com.cygnus.ipoten.recommendation.service.response;

public record JobRecommendedTermView(
        Long termId,
        String title,
        String description,
        Integer rankNo,
        Long categoryId,
        String categoryName
) {
}
