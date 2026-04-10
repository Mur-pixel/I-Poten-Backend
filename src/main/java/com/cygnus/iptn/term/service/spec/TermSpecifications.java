package com.cygnus.iptn.term.service.spec;

import com.cygnus.iptn.term.entity.Term;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public class TermSpecifications {
    private TermSpecifications() {}

    public static Specification<Term> inCategoryIds(Collection<Long> catIds) {
        if(catIds == null || catIds.isEmpty()) return alwaysFalse();
        return (root, cq, cb) -> root.get("category_id").get("id").in(catIds);
    }

    public static Specification<Term> titleContainsIgnoreCase(String q) {
        if(q==null || q.isBlank()) return null;
        final String like = "%" + q.trim().toLowerCase() + "%";
        return (root, cq, cb) -> cb.like(root.get("title"), like);
    }

    public static Specification<Term> alwaysFalse() {
        return (root, cq, cb) -> cb.disjunction();
    }
}
