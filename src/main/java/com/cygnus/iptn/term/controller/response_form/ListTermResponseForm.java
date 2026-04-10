package com.cygnus.iptn.term.controller.response_form;

import com.cygnus.iptn.term.service.response.ListTermResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;


@Getter
@RequiredArgsConstructor
public class ListTermResponseForm {

    private final List<Map<String, Object>> termList;
    private final Long totalItems;
    private final Integer totalPages;

    public static ListTermResponseForm from(final ListTermResponse response) {
        List<Map<String, Object>> combinedTermList = response.transformToResponseForm();
        return new ListTermResponseForm(
                combinedTermList,
                response.getTotalItems(),
                response.getTotalPages());
    }
}
