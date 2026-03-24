package com.cygnus.ipoten.recommendation.controller.response_form;

import com.cygnus.ipoten.recommendation.entity.JobRecommendedTerm;

import java.util.List;

public record JobRecommendedTermsResponseForm(List<Item> items) {

    public static JobRecommendedTermsResponseForm from(List<JobRecommendedTerm> recommendations) {
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
        public static Item from(JobRecommendedTerm recommendation) {
            var term = recommendation.getTerm();
            var category = recommendation.getTermCategory();

            return new Item(
                    term != null ? term.getId() : null,
                    term != null ? term.getId() : null,
                    term != null ? term.getTitle() : null,
                    term != null ? term.getDescription() : null,
                    recommendation.getRankNo(),
                    category != null ? category.getId() : null,
                    category != null ? category.getName() : null
            );
        }
    }
}
