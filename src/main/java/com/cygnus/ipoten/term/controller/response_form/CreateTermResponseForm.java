package com.cygnus.ipoten.term.controller.response_form;

import com.cygnus.ipoten.term.service.response.CreateTermResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// 용어 등록 성공 메시지
// 제목, 설명, 태그

@Getter
@AllArgsConstructor
public class CreateTermResponseForm {

    private final String message;
    private final Long termId;
    private final String title;
    private final String description;
    private final List<String> tags;

    public static CreateTermResponseForm from(CreateTermResponse response) {
        return new CreateTermResponseForm(
                response.getMessage(),
                response.getTermId(),
                response.getTitle(),
                response.getDescription(),
                response.getTags()
        );
    }

}
