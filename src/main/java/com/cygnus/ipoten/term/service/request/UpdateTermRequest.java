package com.cygnus.ipoten.term.service.request;

import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term.entity.Term;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class UpdateTermRequest {
    private final Long termId;
    private final String title;
    private final String description;
    private final String tags;         // ex: "#HTML #DOM"
    private final Long categoryId;

    public Term toUpdateTerm(TermCategory termCategory) {
        return new Term(
                title,
                description,
                termCategory
        );
    }

}
