package com.cygnus.ipoten.wordbook.controller.response_form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class MyWordbookListResponseForm {
    private final List<WordbookSummaryResponseForm> folders;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Item {
        private final Long id;
        private final String name;
        private final long termCount;
    }
}
