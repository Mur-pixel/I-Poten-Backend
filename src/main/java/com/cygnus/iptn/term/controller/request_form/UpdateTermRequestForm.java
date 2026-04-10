package com.cygnus.iptn.term.controller.request_form;

import com.cygnus.iptn.term.service.request.UpdateTermRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 제목, 설명, 카테고리, 태그 수정 가능
@Getter
@RequiredArgsConstructor
public class UpdateTermRequestForm {

    private final Long termId;
    private final String title;
    private final String description;
    private final String tags;
    private final Long categoryId;


    public UpdateTermRequest toUpdateTermRequest(Long termId) {
        return new UpdateTermRequest(termId, title, description, tags, categoryId);
    }

}
