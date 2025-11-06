package com.cygnus.ipoten.term.service.request;

import com.cygnus.ipoten.term.entity.Category;
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

    public Term toUpdateTerm(Category category) {
        return new Term(
                title,
                description,
                category
        );
    }

}
