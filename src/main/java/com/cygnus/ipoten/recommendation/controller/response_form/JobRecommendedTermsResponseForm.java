package com.cygnus.ipoten.recommendation.controller.response_form;

import com.cygnus.ipoten.recommendation.service.response.JobRecommendedTermView;

import java.util.List;

public record JobRecommendedTermsResponseForm(List<Item> items) {

    public static JobRecommendedTermsResponseForm from(List<JobRecommendedTermView> recommendations) {
        return new JobRecommendedTermsResponseForm(
                recommendations.stream()
                        .map(Item::from)
                        .toList()
        );
    }

    public record Item(
            Long termId,
            Long id,
            String title,
            String description,
            Integer rankNo,
            Long categoryId,
            String categoryName
    ) {
        public static Item from(JobRecommendedTermView recommendation) {
            return new Item(
                    recommendation.termId(),
                    recommendation.termId(),
                    recommendation.title(),
                    recommendation.description(),
                    recommendation.rankNo(),
                    recommendation.categoryId(),
                    recommendation.categoryName()
            );
        }
    }
}
