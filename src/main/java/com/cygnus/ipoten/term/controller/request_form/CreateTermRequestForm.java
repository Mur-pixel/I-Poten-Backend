package com.cygnus.ipoten.term.controller.request_form;
// 용어 등록 (제목, 설명, 태그, 카테고리)

import com.cygnus.ipoten.term.service.request.CreateTermRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateTermRequestForm {
    private final Long categoryId;
    private final String title;
    private final String description;
    private final String tags;

    public CreateTermRequest toCreateTermRequest() {
        return new CreateTermRequest(categoryId, title, description, tags);
    }
}
