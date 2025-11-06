package com.cygnus.ipoten.term.controller.response_form;

import com.cygnus.ipoten.term.service.response.UpdateTermResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UpdateTermResponseForm {

    private final String message;
    private final Long termId;
    private final String title;
    private final String description;
    private final List<String> tags;

    public static UpdateTermResponseForm from(UpdateTermResponse response) {
        return new UpdateTermResponseForm(
                response.getMessage(),
                response.getTermId(),
                response.getTitle(),
                response.getDescription(),
                response.getTags()
        );
    }
}