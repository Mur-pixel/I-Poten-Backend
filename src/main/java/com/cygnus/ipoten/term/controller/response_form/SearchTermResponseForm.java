package com.cygnus.ipoten.term.controller.response_form;

import com.cygnus.ipoten.term.service.response.SearchTermResponse;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchTermResponseForm {
    private long total;
    private List<TermSummaryForm> items;

    public static SearchTermResponseForm from(SearchTermResponse response) {
        return SearchTermResponseForm.builder()
                .total(response.getTotal())
                .items(
                        response.getItems().stream()
                                .map(TermSummaryForm::from)
                                .collect(Collectors.toList())
                )
                .build();
    }
}
