package com.cygnus.iptn.term.service.request;

import com.cygnus.iptn.term_category.entity.TermCategory;
import com.cygnus.iptn.term.entity.Term;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class CreateTermRequest {

    private final Long categoryId;
    private final String title;
    private final String description;
    private final String tags;         // ex: "#HTML #DOM"

    public Term toTerm(TermCategory termCategory) {
        return new Term(
                title,
                description,
                termCategory
        );
    }
}