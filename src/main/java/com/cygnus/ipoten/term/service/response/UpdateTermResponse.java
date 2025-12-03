package com.cygnus.ipoten.term.service.response;

import com.cygnus.ipoten.term_category.entity.TermCategory;
import com.cygnus.ipoten.term.entity.Term;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class UpdateTermResponse {

    private final String message;
    private final Long termId;
    private final String title;
    private final String description;
    private final List<String> tags;

    public static UpdateTermResponse from(Term term, List<String> tagNames, TermCategory termCategory) {
        String message = "용어가 성공적으로 수정되었습니다.";
        return new UpdateTermResponse(
                message,
                term.getId(),
                term.getTitle(),
                term.getDescription(),
                tagNames
        );
    }
}
