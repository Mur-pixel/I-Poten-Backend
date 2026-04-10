package com.cygnus.iptn.recommendation.service.response;

public record JobRecommendedTermView(
        Long termId,
        String title,
        String description,
        Integer rankNo,
        Long categoryId,
        String categoryName
) {
}
