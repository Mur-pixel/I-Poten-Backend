package com.cygnus.ipoten.term.service.request;

import com.cygnus.ipoten.term.entity.Category;
import com.cygnus.ipoten.term.entity.Term;
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

    public Term toTerm(Category category) {
        return new Term(
                title,
                description,
                category
        );
    }
}